// Gera o PDF de um relatório em Markdown (tabelas GFM e diagramas Mermaid incluídos):
//
//   node relatorios/gerar-pdf.js relatorios/relatorio-tech-challenge-fase02-final.md
//
// O PDF sai ao lado do .md, com o mesmo nome. O Markdown vira HTML pelo `marked` (via npx, versão
// fixada) e o Mermaid é baixado uma vez, também em versão fixada; o Chrome em modo headless monta
// a página, desenha os diagramas e imprime. Defina CHROME_PATH fora do Windows ou se o Chrome estiver
// em outro lugar. Links relativos apontam para o arquivo no GitHub, porque no PDF não há repositório.
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');
const { pathToFileURL } = require('url');

const MARKED = 'marked@15.0.12';
const MERMAID = 'https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.min.js';
const REPOSITORIO = 'https://github.com/felipe11dias/postech-fase2-restaurantes-app';

const [entrada] = process.argv.slice(2);
if (!entrada || !entrada.endsWith('.md')) {
  console.error('Uso: node relatorios/gerar-pdf.js <relatorio.md>');
  process.exit(1);
}
const CHROME = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
if (!fs.existsSync(CHROME)) {
  console.error(`Chrome não encontrado em "${CHROME}". Defina CHROME_PATH com o caminho do executável.`);
  process.exit(1);
}
const raiz = path.resolve(__dirname, '..');
const saida = entrada.replace(/\.md$/, '.pdf');

// Âncora no mesmo formato do GitHub, para o sumário funcionar igual no repositório e no PDF.
const ancora = texto => texto.trim().toLowerCase().replace(/[^\p{L}\p{N}\s-]/gu, '').replace(/\s/g, '-');
const semTags = html => html.replace(/<[^>]+>/g, '')
  .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'");

function linkNoGitHub(href) {
  if (/^(https?:|mailto:|#)/.test(href)) return href;
  const [caminho, fragmento] = href.split('#');
  const alvo = path.relative(raiz, path.resolve(path.dirname(entrada), decodeURIComponent(caminho))).replace(/\\/g, '/');
  const tipo = fs.existsSync(path.join(raiz, alvo)) && fs.statSync(path.join(raiz, alvo)).isDirectory() ? 'tree' : 'blob';
  return `${REPOSITORIO}/${tipo}/main/${alvo}${fragmento ? '#' + fragmento : ''}`;
}

async function main() {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'relatorio-pdf-'));
  try {
    const corpo = execFileSync('npx', ['--yes', MARKED, '--gfm', '-i', entrada],
      { encoding: 'utf8', shell: process.platform === 'win32', maxBuffer: 64 * 1024 * 1024 });
    const html = corpo
      .replace(/<pre><code class="language-mermaid">([\s\S]*?)<\/code><\/pre>/g, '<pre class="mermaid">$1</pre>')
      .replace(/<h([1-6])>([\s\S]*?)<\/h\1>/g, (_, n, titulo) => `<h${n} id="${ancora(semTags(titulo))}">${titulo}</h${n}>`)
      .replace(/href="([^"]+)"/g, (_, href) => `href="${linkNoGitHub(href)}"`);

    const resposta = await fetch(MERMAID);
    if (!resposta.ok) throw new Error(`não foi possível baixar o Mermaid (${resposta.status})`);
    fs.writeFileSync(path.join(tmp, 'mermaid.min.js'), await resposta.text());

    const pagina = path.join(tmp, 'relatorio.html');
    fs.writeFileSync(pagina, `<!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8"><title>${path.basename(entrada, '.md')}</title>
<style>
  @page { size: A4; margin: 16mm 14mm; }
  body { font: 10.5pt/1.5 "Segoe UI", Arial, sans-serif; color: #1f2328; }
  h1 { font-size: 22pt; margin: 0 0 4pt; }
  h2 { font-size: 15pt; border-bottom: 1px solid #d0d7de; padding-bottom: 3pt; margin-top: 18pt; }
  h2[id^="1"], h2[id^="2"], h2[id^="3"], h2[id^="4"], h2[id^="5"], h2[id^="6"], h2[id^="7"], h2[id^="8"],
  h2[id^="9"] { break-before: page; }
  h3 { font-size: 12pt; margin-top: 14pt; break-after: avoid; }
  table { border-collapse: collapse; width: 100%; margin: 6pt 0; font-size: 8.5pt; }
  th, td { border: 1px solid #d0d7de; padding: 3pt 5pt; vertical-align: top; text-align: left; }
  th { background: #f6f8fa; }
  tr { break-inside: avoid; }
  code { font: 8.5pt Consolas, monospace; background: #f6f8fa; padding: 0 2pt; border-radius: 3px; }
  pre { background: #f6f8fa; padding: 6pt; border-radius: 4px; white-space: pre-wrap; break-inside: avoid; }
  pre code { padding: 0; }
  pre.mermaid { background: none; text-align: center; }
  pre.mermaid svg { max-width: 100%; height: auto; }
  blockquote { margin: 6pt 0; padding: 2pt 10pt; border-left: 3px solid #d0d7de; color: #424a53; }
  a { color: #0969da; text-decoration: none; }
  hr { display: none; }
</style>
<script src="mermaid.min.js"></script>
</head><body>
${html}
<script>
  mermaid.initialize({ startOnLoad: false, theme: 'neutral', flowchart: { useMaxWidth: true } });
  mermaid.run().then(() => document.body.setAttribute('data-pronto', '1'));
</script>
</body></html>`);

    execFileSync(CHROME, ['--headless=new', '--disable-gpu', '--no-pdf-header-footer', '--run-all-compositor-stages-before-draw',
      '--virtual-time-budget=20000', `--print-to-pdf=${path.resolve(saida)}`, pathToFileURL(pagina).href], { stdio: 'ignore' });
    console.log(`PDF gerado: ${saida}`);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
}

main().catch(erro => {
  console.error(erro.message);
  process.exit(1);
});
