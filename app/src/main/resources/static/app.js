// Front-end do RecargaLab. Renderiza dados com textContent: a falha de XSS do Lab 3 está só em /contatos.
const token = localStorage.getItem('token');
if (!token) location.href = '/';

const $ = (id) => document.getElementById(id);
const brl = (v) => Number(v).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
let me = null;

async function api(path, opts = {}) {
  const headers = Object.assign({'Authorization': 'Bearer ' + token}, opts.headers || {});
  const r = await fetch(path, Object.assign({}, opts, {headers}));
  if (r.status === 401) { localStorage.removeItem('token'); location.href = '/'; return {}; }
  const text = await r.text();
  let data = text;
  try { data = JSON.parse(text); } catch (_) { /* texto puro */ }
  return {ok: r.ok, status: r.status, data};
}
const json = (method, body) => ({method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)});

function cell(tr, text, cls) { const td = document.createElement('td'); td.textContent = text; if (cls) td.className = cls; tr.appendChild(td); }

async function carregarPerfil() {
  const r = await api('/api/v1/users/me');
  me = r.data;
  $('nome').textContent = me.nome;
  $('saldo').textContent = brl(me.saldo);
  const f = $('form-perfil');
  f.nome.value = me.nome || '';
  f.telefone.value = me.telefone || '';
}

async function carregarExtrato() {
  const w = await api('/api/v1/wallets/me');
  const r = await api('/api/v1/wallets/' + w.data.id + '/statement');
  const tb = $('extrato'); tb.replaceChildren();
  (r.data || []).forEach(t => {
    const tr = document.createElement('tr');
    cell(tr, new Date(t.criadoEm).toLocaleString('pt-BR'));
    cell(tr, t.tipo);
    cell(tr, t.contraparte);
    cell(tr, brl(t.valor), 'num' + (t.valor < 0 ? ' neg' : ''));
    tb.appendChild(tr);
  });
}

async function carregarProdutos() {
  const r = await api('/api/v1/produtos');
  const sel = $('produtos'); sel.replaceChildren();
  r.data.forEach(p => {
    const o = document.createElement('option');
    o.value = p.id; o.textContent = p.nome; o.dataset.preco = p.preco;
    sel.appendChild(o);
  });
}

$('form-recarga').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target, opt = f.produto.selectedOptions[0];
  // O cliente envia preço, desconto e o id do usuário (Lab 4)
  const r = await api('/api/v1/recargas', json('POST', {
    produtoId: opt.value, telefone: f.telefone.value,
    valor: Number(opt.dataset.preco), desconto: 0, userId: me.id
  }));
  $('msg-recarga').textContent = r.ok
    ? `Recarga ${r.data.produto} concluída. Protocolo ${r.data.protocolo}.`
    : 'Falha na recarga: ' + (r.data.erro || r.status);
  carregarPerfil();
});

$('form-busca').addEventListener('submit', async (e) => {
  e.preventDefault();
  const op = e.target.operadora.value;
  const r = await api('/api/v1/recargas/busca?operadora=' + encodeURIComponent(op));
  const tb = $('busca'); tb.replaceChildren();
  if (!r.ok) { const tr = document.createElement('tr'); cell(tr, 'Erro na busca (' + r.status + ')'); tb.appendChild(tr); return; }
  r.data.forEach(x => {
    const tr = document.createElement('tr');
    cell(tr, x.id); cell(tr, x.operadora); cell(tr, x.telefone); cell(tr, brl(x.debitado), 'num'); cell(tr, x.status);
    tb.appendChild(tr);
  });
});

async function carregarCupons() {
  const r = await api('/api/v1/cashback/cupons');
  const ul = $('cupons'); ul.replaceChildren();
  r.data.forEach(c => {
    const li = document.createElement('li');
    const s = document.createElement('span'); s.textContent = `${c.codigo} · ${brl(c.valor)}`;
    const b = document.createElement('button');
    b.textContent = c.usado ? 'Resgatado' : 'Resgatar'; b.disabled = c.usado;
    b.addEventListener('click', async () => {
      const x = await api('/api/v1/cashback/resgatar', json('POST', {codigo: c.codigo}));
      $('msg-cashback').textContent = x.ok ? `Cupom resgatado: ${brl(x.data.valor)}.` : 'Não foi possível resgatar: ' + (x.data.erro || x.status);
      carregarCupons(); carregarPerfil();
    });
    li.append(s, b); ul.appendChild(li);
  });
}

async function carregarContatos() {
  const r = await api('/api/v1/contatos');
  const ul = $('contatos'); ul.replaceChildren();
  r.data.forEach(c => {
    const li = document.createElement('li');
    const nick = document.createElement('input'); nick.value = c.apelido; nick.setAttribute('aria-label', 'Apelido');
    const tel = document.createElement('input'); tel.value = c.telefone; tel.setAttribute('aria-label', 'Telefone');
    const b = document.createElement('button'); b.textContent = 'Salvar';
    b.addEventListener('click', async () => {
      const x = await api('/api/v1/contatos/' + c.id, json('PUT', {apelido: nick.value, telefone: tel.value}));
      b.textContent = x.ok ? 'Salvo' : 'Erro';
      setTimeout(() => (b.textContent = 'Salvar'), 1500);
    });
    li.append(nick, tel, b); ul.appendChild(li);
  });
}

$('form-perfil').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  const r = await api('/api/v1/users/me', json('PUT', {nome: f.nome.value, telefone: f.telefone.value}));
  $('msg-perfil').textContent = r.ok ? 'Perfil atualizado.' : 'Erro ao salvar (' + r.status + ').';
  carregarPerfil();
});

$('form-comprovante').addEventListener('submit', async (e) => {
  e.preventDefault();
  const body = new URLSearchParams({url: e.target.url.value});
  const r = await api('/api/v1/receipts/import', {method: 'POST', headers: {'Content-Type': 'application/x-www-form-urlencoded'}, body});
  $('comprovante').textContent = typeof r.data === 'string' ? r.data : JSON.stringify(r.data, null, 2);
});

$('sair').addEventListener('click', () => {
  localStorage.removeItem('token');
  document.cookie = 'token=; Max-Age=0; path=/';
  location.href = '/';
});

const loaders = {extrato: carregarExtrato, recargas: carregarProdutos, cashback: carregarCupons, contatos: carregarContatos, perfil: carregarPerfil, comprovantes: async () => {}};
function rota() {
  const nome = (location.hash || '#extrato').slice(1);
  const alvo = loaders[nome] ? nome : 'extrato';
  document.querySelectorAll('.view').forEach(v => v.classList.toggle('on', v.id === 's-' + alvo));
  document.querySelectorAll('#tabs a').forEach(a => a.classList.toggle('on', a.getAttribute('href') === '#' + alvo));
  loaders[alvo]();
}
window.addEventListener('hashchange', rota);
carregarPerfil().then(rota);
