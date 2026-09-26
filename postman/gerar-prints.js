// Gera os prints da coleção (postman/prints/NN-nome.png) a partir de UMA execução do Newman:
// cada imagem mostra o request, a resposta e os testes daquela chamada.
//
//   npx newman run postman/Restaurantes.postman_collection.json --reporters cli,json \
//       --reporter-json-export target/newman.json
//   node postman/gerar-prints.js target/newman.json postman/Restaurantes.postman_collection.json postman/prints
//
// Usa o Chrome em modo headless (CHROME_PATH, se não estiver no caminho padrão do Windows).
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const [relatorio, colecaoPath, saida] = process.argv.slice(2);
const run = JSON.parse(fs.readFileSync(relatorio, 'utf8')).run;
const colecao = JSON.parse(fs.readFileSync(colecaoPath, 'utf8'));
const CHROME = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const LARGURA = 1200;
const ALTURA_LINHA = 19;

const pastaDe = {};
colecao.item.forEach((pasta) => pasta.item.forEach((req) => { pastaDe[req.name] = pasta.name; }));

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const abreviaJwt = (s) => String(s).replace(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g,
  (jwt) => jwt.slice(0, 20) + '…(JWT abreviado)');

function url(u) {
  const porta = u.port ? ':' + u.port : '';
  const query = (u.query || []).filter((q) => !q.disabled).map((q) => `${q.key}=${q.value}`).join('&');
  return `${u.protocol}://${u.host.join('.')}${porta}/${u.path.join('/')}${query ? '?' + query : ''}`;
}
function formata(texto) {
  if (!texto) return '';
  try { return abreviaJwt(JSON.stringify(JSON.parse(texto), null, 2)); } catch { return abreviaJwt(texto); }
}
function cabecalhos(lista, chaves) {
  return lista.filter((h) => !h.system && chaves.includes(h.key.toLowerCase()))
    .map((h) => `${h.key}: ${abreviaJwt(h.value)}`).join('\n');
}

fs.mkdirSync(saida, { recursive: true });
fs.readdirSync(saida).filter((f) => f.endsWith('.png')).forEach((f) => fs.unlinkSync(path.join(saida, f)));
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'prints-'));

run.executions.forEach((ex, i) => {
  const nome = ex.item.name;
  const req = ex.request;
  const resp = ex.response;
  const corpoReq = formata(req.body && req.body.raw);
  const corpoResp = formata(resp.stream ? Buffer.from(resp.stream.data).toString('utf8') : '');
  const hReq = cabecalhos(req.header, ['content-type', 'authorization']);
  const hResp = cabecalhos(resp.header, ['content-type', 'location']);
  const asserts = ex.assertions || [];
  const classe = resp.code < 300 ? 'ok' : resp.code < 500 ? 'aviso' : 'erro';
  const bloco = (titulo, conteudo) => conteudo
    ? `<h3>${titulo}</h3><pre>${esc(conteudo)}</pre>` : '';

  const html = `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><style>
    body{margin:0;background:#f4f5f7;font:14px/1.45 "Segoe UI",Arial,sans-serif;color:#1f2328}
    .card{margin:16px;background:#fff;border:1px solid #d0d7de;border-radius:8px;overflow:hidden}
    .topo{padding:12px 18px;border-bottom:1px solid #d0d7de;background:#fafbfc}
    .pasta{color:#57606a;font-size:12px}
    .nome{font-size:18px;font-weight:600;margin-top:2px}
    .linha{display:flex;align-items:center;gap:10px;padding:10px 18px;border-bottom:1px solid #eaeef2;font-family:Consolas,monospace;font-size:13px}
    .metodo{font-weight:700;color:#bc4c00}
    .url{flex:1;word-break:break-all}
    .status{font-weight:700;padding:2px 10px;border-radius:12px;font-family:"Segoe UI",Arial,sans-serif}
    .ok{background:#dafbe1;color:#116329}.aviso{background:#fff1e5;color:#953800}.erro{background:#ffebe9;color:#a40e26}
    .tempo{color:#57606a;font-family:"Segoe UI",Arial,sans-serif}
    .cols{display:grid;grid-template-columns:1fr 1fr}
    .col{padding:4px 18px 12px;min-width:0}.col+.col{border-left:1px solid #eaeef2}
    h3{margin:10px 0 4px;font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:#57606a}
    pre{margin:0;padding:8px 10px;background:#f6f8fa;border-radius:6px;font:13px/${ALTURA_LINHA}px Consolas,monospace;white-space:pre-wrap;word-break:break-all}
    .testes{padding:6px 18px 14px;border-top:1px solid #eaeef2}
    .t{font-size:13px}.passou{color:#116329}.falhou{color:#a40e26}
    .rodape{padding:6px 18px;color:#8c959f;font-size:11px;border-top:1px solid #eaeef2}
  </style></head><body><div class="card">
    <div class="topo"><div class="pasta">${esc(pastaDe[nome] || '')}</div><div class="nome">${esc(nome)}</div></div>
    <div class="linha"><span class="metodo">${req.method}</span><span class="url">${esc(url(req.url))}</span>
      <span class="status ${classe}">${resp.code} ${esc(resp.status)}</span><span class="tempo">${resp.responseTime} ms</span></div>
    <div class="cols">
      <div class="col">${bloco('Cabeçalhos da requisição', hReq)}${bloco('Corpo da requisição', corpoReq) || '<h3>Corpo da requisição</h3><pre>(sem corpo)</pre>'}</div>
      <div class="col">${bloco('Cabeçalhos da resposta', hResp)}${bloco('Corpo da resposta', corpoResp) || '<h3>Corpo da resposta</h3><pre>(sem corpo)</pre>'}</div>
    </div>
    <div class="testes"><h3>Testes (${asserts.filter((a) => !a.error).length}/${asserts.length})</h3>
      ${asserts.map((a) => `<div class="t ${a.error ? 'falhou' : 'passou'}">${a.error ? '✗' : '✓'} ${esc(a.assertion)}</div>`).join('')}</div>
    <div class="rodape">Execução Newman de ${new Date(run.timings.started).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' })} · requisição ${i + 1} de ${run.executions.length}</div>
  </div><script>document.body.setAttribute("data-altura", Math.ceil(document.documentElement.getBoundingClientRect().height))</script></body></html>`;

  const arquivoHtml = path.join(tmp, `${i}.html`);
  fs.writeFileSync(arquivoHtml, html);
  const slug = nome.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  const png = path.resolve(saida, `${slug}.png`);
  // Primeira passada mede a altura real renderizada; a segunda fotografa exatamente esse tamanho.
  const dom = execFileSync(CHROME, ["--headless=new", "--disable-gpu", `--window-size=${LARGURA},600`, "--dump-dom",
    "file:///" + arquivoHtml.replace(/\\/g, "/")], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  const altura = Number(/data-altura="(\d+)"/.exec(dom)[1]);
  execFileSync(CHROME, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1',
    `--window-size=${LARGURA},${altura}`, `--screenshot=${png}`, 'file:///' + arquivoHtml.replace(/\\/g, '/')],
  { stdio: 'ignore' });
});
console.log(`${run.executions.length} prints em ${saida}`);
