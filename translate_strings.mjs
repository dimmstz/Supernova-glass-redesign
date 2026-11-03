import fs from 'node:fs'
import { pathToFileURL } from 'node:url'
const { LlmClient, messageText } = await import(pathToFileURL('/home/ubuntu/skills/builtin-llm-models/lib/llm.mjs').href)
const items = JSON.parse(fs.readFileSync('/tmp/lumi_strings_to_translate.json', 'utf8'))
const outPath = '/tmp/lumi_ptbr_translations.json'
const results = fs.existsSync(outPath) ? JSON.parse(fs.readFileSync(outPath, 'utf8')) : {}
const batchSize = 45
const llm = new LlmClient({ timeoutMs: 300000, maxRetries: 3 })
const schema = {
  type: 'object',
  additionalProperties: false,
  required: ['translations'],
  properties: {
    translations: {
      type: 'array',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['index', 'translation'],
        properties: { index: { type: 'integer' }, translation: { type: 'string' } }
      }
    }
  }
}
for (let start = 0; start < items.length; start += batchSize) {
  const batch = items.slice(start, start + batchSize)
  const pending = batch.map((x, i) => ({ index: start+i, id:x.id, kind:x.kind, quantity:x.quantity, source:x.text }))
  const todo = pending.filter(x => !results[String(x.index)])
  if (!todo.length) continue
  console.error(`Translating ${start+1}-${start+todo.length}/${items.length}`)
  const prompt = `Traduza os valores de recursos Android abaixo do inglês para português brasileiro natural, curto e adequado a uma interface de launcher. Retorne exatamente um objeto JSON no schema solicitado, com uma tradução para cada index. Preserve rigorosamente placeholders como %1$s, %1$d, %2$d, símbolos, unidades (dp/sp), \n, aspas tipográficas quando fizerem sentido e nomes próprios. Não traduza nomes de apps, marcas, URLs, comandos, acrônimos ou valores que sejam apenas letras de dias da semana. Para quantity one/other, use singular/plural correto em pt-BR. Não inclua explicações.\n\nITENS:\n${JSON.stringify(todo, null, 2)}`
  const resp = await llm.chat({
    model: 'gpt-6-luna',
    messages: [
      { role:'system', content:'Você é um tradutor técnico especializado em Android, português brasileiro e localização de interfaces.' },
      { role:'user', content:prompt }
    ],
    reasoning: { effort: 'low' },
    max_completion_tokens: 14000,
    response_format: { type:'json_schema', json_schema:{ name:'android_translation_batch', strict:true, schema } }
  })
  const parsed = JSON.parse(messageText(resp))
  for (const t of parsed.translations) {
    if (!todo.some(x => x.index === t.index)) throw new Error(`unexpected index ${t.index}`)
    results[String(t.index)] = t.translation
  }
  if (parsed.translations.length !== todo.length) throw new Error(`missing translations: got ${parsed.translations.length}, expected ${todo.length}`)
  fs.writeFileSync(outPath, JSON.stringify(results, null, 2))
}
console.log(`translated ${Object.keys(results).length}/${items.length}`)
