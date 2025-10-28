/*!
* Start Bootstrap - The Big Picture v5.0.6 (https://startbootstrap.com/template/the-big-picture)
* Copyright 2013-2023 Start Bootstrap
* Licensed under MIT (https://github.com/StartBootstrap/startbootstrap-the-big-picture/blob/master/LICENSE)
*/

// Foco no e-mail ao abrir o offcanvas de login
document.getElementById('offcanvasRight')
    ?.addEventListener('shown.bs.offcanvas', function () {
        document.getElementById('loginEmail')?.focus();
    });

// Modulo de Busca no index
// Busca pública no index: lista com botão "Fazer este simulado" por item
(function () {
    const input = document.getElementById('buscaProva');
    if (!input) return; // só roda no index

    const resultDiv = document.getElementById('resultadoBusca');
    const card = document.getElementById('cardProvaSelecionada');
    const tituloEl = document.getElementById('tituloProva');
    const btnInferior = document.getElementById('btnFazerProva'); // vamos usar para "Pesquisa avançada"

    // Já deixa o botão inferior como "Pesquisa avançada" ao carregar
    if (btnInferior) {
        btnInferior.textContent = 'Pesquisa avançada';
        btnInferior.href = '/provas';
    }

    let t = null;

    function render(items) {
        // mantém o botão inferior como "Pesquisa avançada"
        if (btnInferior) {
            btnInferior.textContent = 'Pesquisa avançada';
            btnInferior.href = '/provas';
        }

        if (!items || items.length === 0) {
            if (resultDiv) resultDiv.innerHTML = '<span class="text-muted">Nenhuma prova encontrada.</span>';
            if (card) card.classList.add('d-none');
            return;
        }

        // Mostra o título da primeira prova no card (apenas como destaque visual)
        const p0 = items[0];
        if (tituloEl) tituloEl.textContent = p0.titulo;
        if (card) card.classList.remove('d-none');

        // Lista com link do título (detalhes) + botão "Fazer este simulado"
        if (resultDiv) {
            resultDiv.innerHTML = items.map(p => `
        <div class="py-2 border-bottom d-flex justify-content-between align-items-center gap-2">
          <div class="me-2">
            <a class="link-primary text-decoration-none" href="/provas/${p.id}"> ${p.titulo} </a>
            <div class="small text-muted">Professor: ${p.professorNome ?? ''}</div>
          </div>
          <a class="btn btn-sm btn-primary" href="/provas/${p.id}">Fazer este simulado</a>
        </div>
      `).join('');
        }
    }

    input.addEventListener('input', () => {
        const q = input.value.trim();
        clearTimeout(t);

        if (q.length === 0) {
            if (resultDiv) resultDiv.innerHTML = '';
            if (card) card.classList.add('d-none');
            return;
        }

        t = setTimeout(async () => {
            try {
                const res = await fetch(`/api/provas/search?q=${encodeURIComponent(q)}`);
                const data = await res.json();
                render(data);
            } catch (e) {
                console.error(e);
            }
        }, 250); // debounce
    });
})();

//Scripts de Login
// Rótulos flutuantes: aplica .has-value quando houver conteúdo
document.querySelectorAll('.input-wrapper input').forEach(inp => {
    const apply = () => inp.classList.toggle('has-value', inp.value.trim().length > 0);
    inp.addEventListener('input', apply);
    inp.addEventListener('blur', apply);
    apply();
});

// Toggle de senha com ícone
(function () {
    const input = document.getElementById('loginSenha');
    const btn = document.getElementById('toggleSenha');
    const eye = btn?.querySelector('.eye-icon');
    if (!input || !btn || !eye) return;

    btn.addEventListener('click', () => {
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        eye.classList.toggle('show-password', show);
        btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    });
})();

// “loading” no submit
document.getElementById('formLoginProf')?.addEventListener('submit', (e) => {
    const btn = e.currentTarget.querySelector('.login-btn');
    btn?.classList.add('loading');
    setTimeout(() => btn?.classList.remove('loading'), 1500);
});


//Script para minhas questões
// Habilita/desabilita o radio conforme a alternativa possui texto
function toggleRadio(radioId, texto) {
    const r = document.getElementById(radioId);
    const ok = (texto || '').trim().length > 0;
    if (!r) return;
    r.disabled = !ok;
    if (!ok && r.checked) r.checked = false;
}

// Ao carregar a página, garante consistência (especialmente no editar)
document.addEventListener('DOMContentLoaded', function () {
    ['A','B','C','D','E'].forEach(function (letra) {
        const edit = document.getElementById('correta_edit_' + letra);
        const novo = document.getElementById('correta_new_' + letra);
        const campoA = document.querySelector('[name="alternativa' + letra + '"]');
        if (campoA && (edit || novo)) toggleRadio((edit || novo).id, campoA.value);
    });
});


//Script para criação de provas e utilização dos efeitos e ações dentro da mesma área

// ================== PROVA: montar seleção de questões (Create/Edit) ==================
(function () {
    'use strict';

    // qual form está na página?
    const SUF = document.getElementById('provaFormE') ? 'E' :
        document.getElementById('provaFormC') ? 'C' : null;
    if (!SUF) return;

    // ELEMENTOS (por sufixo)
    const listaSel   = document.getElementById('listaSel'   + SUF);
    const hiddenBox  = document.getElementById('itensHidden'+ SUF);
    const painel     = document.getElementById('painelBusca'+ SUF);
    const btnAbrir   = document.getElementById('btnAbrirBusca' + SUF);
    const btnFechar  = document.getElementById('btnFecharBusca'+ SUF);
    const btnAddSel  = document.getElementById('btnAddSel'     + SUF);
    const btnLimpar  = document.getElementById('btnLimparSel'  + SUF);
    const fTexto     = document.getElementById('buscarTexto'   + SUF);
    const fArea      = document.getElementById('buscarArea'    + SUF);
    const fMinhas    = document.getElementById('buscarMinhas'  + SUF);
    const fPublicas  = document.getElementById('buscarPublicas'+ SUF);
    const resQ       = document.getElementById('resQ' + SUF);
    const pagQ       = document.getElementById('pagQ' + SUF);

    // estado
    let sel = []; // [{id, enunciado}]
    let debTimer = null;
    let ctl = null;

    // utils
    const escapeHtml = (s) => (s || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const trunc = (t, n=160) => { const s = (t||'').trim(); return s.length > n ? s.slice(0,n)+'…' : (s || '(sem enunciado)'); };
    const show = el => el && el.classList.remove('d-none');
    const hide = el => el && el.classList.add('d-none');

    function rebuildHiddenInputs() {
        if (!hiddenBox) return;
        hiddenBox.innerHTML = '';
        sel.forEach(x => {
            const inp = document.createElement('input');
            inp.type = 'hidden';
            inp.name = 'itens';   // vários inputs com o mesmo nome → List<Long> no controller
            inp.value = x.id;
            hiddenBox.appendChild(inp);
        });
    }

    function addToSel(it) {
        const idNum = Number(it.id);
        if (!sel.some(s => s.id === idNum)) {
            sel.push({ id: idNum, enunciado: it.enunciado || '' });
            renderSel();
        }
    }

    function renderSel() {
        listaSel.innerHTML = '';
        sel.forEach(it => {
            const full = (it.enunciado || '').trim();
            const row = document.createElement('div');
            row.className = 'list-group-item d-flex justify-content-between align-items-center selQ';
            row.innerHTML = `
        <div class="d-flex align-items-center w-100">
          <span class="id">#${it.id}</span>
          <span class="txt" title="${escapeHtml(full)}">${escapeHtml(trunc(full, 120))}</span>
        </div>
        <button class="btn btn-sm btn-outline-danger" type="button">Remover</button>
      `;
            row.querySelector('button').onclick = () => {
                sel = sel.filter(x => x.id !== it.id);
                renderSel();
            };
            listaSel.appendChild(row);
        });
        rebuildHiddenInputs();
    }

    // linha de resultado da busca
    function makeResultRow(it) {
        const full = (it.enunciado || '').trim();
        const row = document.createElement('div');
        row.className = 'list-group-item qrow';
        row.dataset.id = it.id;
        row.dataset.enunciado = it.enunciado || '';

        row.innerHTML = `
      <div class="form-check me-2">
        <input class="form-check-input selck" type="checkbox" value="${it.id}">
      </div>
      <div class="flex-grow-1">
        <div class="small mb-1">
          <span class="badge bg-secondary me-1">${escapeHtml(it.area || 'Área')}</span>
          <span class="badge bg-success-subtle text-success border me-1">${it.publica ? 'Pública' : 'Privada'}</span>
          <span class="text-muted">#${it.id}</span>
        </div>
        <div class="excerpt" title="${escapeHtml(full)}">${escapeHtml(trunc(full))}</div>
      </div>
      <div class="ms-2">
        <button class="btn btn-sm btn-primary add-one" type="button">Adicionar</button>
      </div>
    `;

        row.querySelector('.add-one').onclick = () => {
            addToSel({ id: row.dataset.id, enunciado: row.dataset.enunciado });
            row.remove(); // some da lista
        };

        return row;
    }

    // busca no servidor
    function carregarBusca(page = 0) {
        if (!resQ || !pagQ) return;

        const texto = (fTexto?.value || '').trim();
        const area  = (fArea?.value || '').trim();
        const minhas = !!fMinhas?.checked;
        const publicas = !!fPublicas?.checked;

        const p = new URLSearchParams();
        p.set('page', String(Math.max(0, page)));
        p.set('size', '10');
        p.set('minhas',  minhas ? 'true' : 'false');
        p.set('publicas', publicas ? 'true' : 'false');
        if (texto) p.set('q', texto);
        if (area)  p.set('areaId', area);

        if (ctl) ctl.abort();
        ctl = new AbortController();

        resQ.innerHTML = '<div class="text-muted">Carregando…</div>';
        pagQ.innerHTML = '';

        fetch('/prof/questoes/api/buscar?' + p.toString(), { signal: ctl.signal })
            .then(async r => { if (!r.ok) throw new Error('HTTP '+r.status+' - '+await r.text()); return r.json(); })
            .then(d => {
                const itens = (d.content || []).filter(it => !sel.some(s => s.id === Number(it.id)));
                if (!itens.length) {
                    resQ.innerHTML = '<div class="text-muted">Nenhuma questão encontrada.</div>';
                    return;
                }

                resQ.innerHTML = '';
                itens.forEach(it => resQ.appendChild(makeResultRow(it)));

                const total = d.totalPages || 0, cur = d.number || 0;
                pagQ.innerHTML = '';
                if (total > 1) {
                    const mk = (lbl, cls, dis, cb) => {
                        const b = document.createElement('button');
                        b.className = 'btn btn-sm ' + cls; b.textContent = lbl; b.disabled = dis; b.onclick = cb; return b;
                    };
                    pagQ.appendChild(mk('«','btn-outline-secondary me-1', cur===0, () => carregarBusca(cur-1)));
                    for (let i=0;i<total;i++) pagQ.appendChild(mk(String(i+1), i===cur?'btn-primary':'btn-outline-secondary', false, () => carregarBusca(i)));
                    pagQ.appendChild(mk('»','btn-outline-secondary ms-1', cur===total-1, () => carregarBusca(cur+1)));
                }
            })
            .catch(err => {
                if (err.name === 'AbortError') return;
                resQ.innerHTML = `<div class="alert alert-danger">Erro ao buscar: ${escapeHtml(err.message)}</div>`;
                pagQ.innerHTML = '';
            });
    }

    // eventos
    btnAbrir?.addEventListener('click', () => { show(painel); carregarBusca(0); });
    btnFechar?.addEventListener('click', () => hide(painel));
    btnLimpar?.addEventListener('click', () => { sel = []; renderSel(); });

    // adicionar selecionadas
    btnAddSel?.addEventListener('click', () => {
        const checks = resQ.querySelectorAll('.selck:checked');
        if (!checks.length) return;
        checks.forEach(ch => {
            const row = ch.closest('[data-id]');
            if (!row) return;
            addToSel({ id: row.dataset.id, enunciado: row.dataset.enunciado });
            row.remove(); // tira da lista
        });
    });

    // filtros com debounce no texto
    const debounce = (fn, t=350) => { clearTimeout(debTimer); debTimer = setTimeout(fn, t); };
    fTexto?.addEventListener('input', () => debounce(() => carregarBusca(0)));
    fArea?.addEventListener('change', () => carregarBusca(0));
    fMinhas?.addEventListener('change', () => carregarBusca(0));
    fPublicas?.addEventListener('change', () => carregarBusca(0));

    // init: cria/edita
    (function init() {
        // 1) ler inputs hidden existentes (no EDITAR já vêm do servidor; no CRIAR, vazio)
        const exist = hiddenBox ? Array.from(hiddenBox.querySelectorAll('input[name="itens"]')) : [];
        sel = exist.map(inp => ({ id: Number(inp.value), enunciado: '' }));

        // 2) no EDITAR: hidratar enunciados (e preencher se não havia inputs)
        if (SUF === 'E') {
            const provaId = document.getElementById('provaFormE')?.dataset?.provaId;
            if (provaId) {
                fetch(`/prof/provas/${provaId}/itens`)
                    .then(r => r.ok ? r.json() : Promise.reject(new Error('Falha ao carregar itens')))
                    .then(d => {
                        const items = d.content || [];
                        if (sel.length === 0) {
                            sel = items.map(it => ({ id: Number(it.id), enunciado: it.enunciado || '' }));
                        } else {
                            const map = new Map(items.map(it => [Number(it.id), (it.enunciado || '')]));
                            sel = sel.map(x => ({ id: x.id, enunciado: map.get(x.id) || '' }));
                        }
                        renderSel();
                    })
                    .catch(() => renderSel());
                return;
            }
        }
        renderSel();
    })();
})();

//Filtro para  minhas questoes
(function(){
    const form = document.getElementById('filtroQ');
    if (!form) return;
    const q = form.querySelector('input[name="q"]');
    const area = form.querySelector('select[name="areaId"]');
    let t;
    if (q) q.addEventListener('input', ()=>{ clearTimeout(t); t=setTimeout(()=>form.submit(), 500); });
    if (area) area.addEventListener('change', ()=>form.submit());
})();


// auto-submit ao digitar (500ms)
(function(){
    const f = document.getElementById('filtroP');
    if (!f) return;
    const q = f.querySelector('input[name="q"]');
    let t;
    if (q) q.addEventListener('input', ()=>{ clearTimeout(t); t=setTimeout(()=>f.submit(), 500); });
})();

// ======================= BUSCA (EDITAR) =======================
function montarUrlBuscaE(page = 0) {
    const texto = document.getElementById('buscarTextoE')?.value.trim() || '';
    const area  = document.getElementById('buscarAreaE')?.value || '';
    const minhas = document.getElementById('buscarMinhasE')?.checked ?? true;
    const publicas = document.getElementById('buscarPublicasE')?.checked ?? true;

    // NOVOS selects:
    const dificuldade  = document.getElementById('buscarDificuldadeE')?.value || '';
    const escolaridade = document.getElementById('buscarEscolaridadeE')?.value || '';

    const params = new URLSearchParams();
    if (texto) params.set('q', texto);
    if (area) params.set('areaId', area);
    params.set('minhas', minhas);
    params.set('publicas', publicas);
    params.set('page', page);
    params.set('size', 10);

    if (dificuldade)  params.set('dificuldade', dificuldade);     // << NOVO
    if (escolaridade) params.set('escolaridade', escolaridade);    // << NOVO

    return `/prof/questoes/api/buscar?` + params.toString();
}

// Chamada Ajax (use onde você já carrega a lista E):
async function carregarBuscaE(page = 0) {
    const url = montarUrlBuscaE(page);
    const res = await fetch(url);
    const data = await res.json();

}

// Listeners (quando o usuário muda os filtros em E):
['buscarTextoE','buscarAreaE','buscarMinhasE','buscarPublicasE',
    'buscarDificuldadeE','buscarEscolaridadeE'  // << NOVO
].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', () => carregarBuscaE(0));
    if (el && (el.tagName === 'SELECT' || el.type === 'checkbox')) {
        el.addEventListener('change', () => carregarBuscaE(0));
    }
});


// ======================= BUSCA (CRIAR) =======================
function montarUrlBuscaC(page = 0) {
    const texto = document.getElementById('buscarTextoC')?.value.trim() || '';
    const area  = document.getElementById('buscarAreaC')?.value || '';
    const minhas = document.getElementById('buscarMinhasC')?.checked ?? true;
    const publicas = document.getElementById('buscarPublicasC')?.checked ?? true;

    // NOVOS selects:
    const dificuldade  = document.getElementById('buscarDificuldadeC')?.value || '';
    const escolaridade = document.getElementById('buscarEscolaridadeC')?.value || '';

    const params = new URLSearchParams();
    if (texto) params.set('q', texto);
    if (area) params.set('areaId', area);
    params.set('minhas', minhas);
    params.set('publicas', publicas);
    params.set('page', page);
    params.set('size', 10);

    if (dificuldade)  params.set('dificuldade', dificuldade);     // << NOVO
    if (escolaridade) params.set('escolaridade', escolaridade);    // << NOVO

    return `/prof/questoes/api/buscar?` + params.toString();
}

async function carregarBuscaC(page = 0) {
    const url = montarUrlBuscaC(page);
    const res = await fetch(url);
    const data = await res.json();
}

['buscarTextoC','buscarAreaC','buscarMinhasC','buscarPublicasC',
    'buscarDificuldadeC','buscarEscolaridadeC'  // << NOVO
].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', () => carregarBuscaC(0));
    if (el && (el.tagName === 'SELECT' || el.type === 'checkbox')) {
        el.addEventListener('change', () => carregarBuscaC(0));
    }
});

// Se já existem botões para abrir o painel e carregar a 1ª página:
document.getElementById('btnAbrirBuscaE')?.addEventListener('click', () => {
    document.getElementById('painelBuscaE')?.classList.remove('d-none');
    carregarBuscaE(0);
});
document.getElementById('btnAbrirBuscaC')?.addEventListener('click', () => {
    document.getElementById('painelBuscaC')?.classList.remove('d-none');
    carregarBuscaC(0);
});



// === Configurar Simulado: limitar quantidade ao disponível ===
(function () {
    // só roda na página com o form de iniciar
    const form = document.querySelector('form[action="/iniciar"]');
    if (!form) return;

    const qtyInputs = form.querySelectorAll('input[name="quantidade"]');

    // enquanto digita, mantém entre 0 e data-max
    qtyInputs.forEach(inp => {
        const max = parseInt(inp.dataset.max || '0', 10);

        inp.addEventListener('input', () => {
            let v = parseInt(inp.value || '0', 10);
            if (Number.isNaN(v) || v < 0) v = 0;
            if (v > max) v = max;
            inp.value = v;
        });
    });

    // última checagem antes de enviar
    form.addEventListener('submit', (e) => {
        let ok = true;
        qtyInputs.forEach(inp => {
            const max = parseInt(inp.dataset.max || '0', 10);
            const v = parseInt(inp.value || '0', 10);
            if (v > max) {
                inp.reportValidity?.();
                ok = false;
            }
        });
        if (!ok) e.preventDefault();
    });
})();
