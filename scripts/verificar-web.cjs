const fs = require('node:fs');
const path = require('node:path');
const output = path.resolve('artifacts');
fs.mkdirSync(output, { recursive: true });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
(async () => {
  const targets = await (await fetch('http://127.0.0.1:9222/json')).json();
  const target = targets.find(item => item.type === 'page');
  if (!target) throw new Error('Nenhuma página no navegador de teste');
  const socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  let id = 0;
  const pending = new Map();
  const externas = [];
  socket.onmessage = event => {
    const message = JSON.parse(event.data);
    if (message.method === 'Fetch.requestPaused') {
      const local = new URL(message.params.request.url).origin === 'http://127.0.0.1:8081';
      if (!local) externas.push(message.params.request.url);
      socket.send(JSON.stringify({ id: ++id,
        method: local ? 'Fetch.continueRequest' : 'Fetch.failRequest',
        params: local ? { requestId: message.params.requestId }
          : { requestId: message.params.requestId, errorReason: 'InternetDisconnected' }
      }));
      return;
    }
    const handler = pending.get(message.id);
    if (!handler) return;
    pending.delete(message.id);
    message.error ? handler.reject(new Error(JSON.stringify(message.error))) : handler.resolve(message.result);
  };
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const messageId = ++id; pending.set(messageId, { resolve, reject });
    socket.send(JSON.stringify({ id: messageId, method, params }));
  });
  const evaluate = async expression => {
    const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
    return result.result.value;
  };
  const waitFor = async expression => {
    for (let attempt = 0; attempt < 60; attempt++) {
      if (await evaluate(expression)) return;
      await delay(250);
    }
    throw new Error('Tela não carregou: ' + expression);
  };
  const navigate = async route => {
    await send('Page.navigate', { url: 'http://127.0.0.1:8081' + route });
    await waitFor('document.readyState === "complete"');
    await delay(250);
  };
  const capture = async name => {
    const result = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true });
    fs.writeFileSync(path.join(output, name + '.png'), Buffer.from(result.data, 'base64'));
  };
  await send('Page.enable');
  await send('Network.deleteCookies', { name: 'bateai_sessao', url: 'http://127.0.0.1:8081/' });
  await send('Fetch.enable', { patterns: [{ urlPattern: '*' }] });
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1040, deviceScaleFactor: 1, mobile: false });
  await navigate('/login');
  await capture('web-login');
  await navigate('/cadastro');
  await capture('web-cadastro');
  const email = 'visual' + Date.now() + '@example.test';
  const fill = fields => evaluate('Object.entries(' + JSON.stringify(fields) + ').forEach(([name,value]) => { const field=document.querySelector("[name="+name+"]"); if(!field)throw new Error(name); field.value=value; }); document.querySelector("form").requestSubmit();');
  await fill({ nome: 'Ana Oliveira', email, senha: 'TesteVisual123!', confirmacao: 'TesteVisual123!' });
  await waitFor('location.pathname === "/login"');
  await fill({ email, senha: 'TesteVisual123!' });
  await waitFor('location.pathname === "/inicio" || location.pathname === "/home" || (location.pathname === "/" && document.body.innerText.includes("Ana"))');
  await delay(350);
  await capture('web-inicio-vazio');
  await evaluate('document.querySelector("form input[value=ENTRADA]").form.requestSubmit();');
  await waitFor('document.body.innerText.includes("Trabalhando")');
  await delay(1300);
  await navigate('/');
  await capture('web-inicio');
  await navigate('/historico');
  await capture('web-historico');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await navigate('/');
  await capture('web-mobile');
  if (!await evaluate('document.querySelector(".logout").getClientRects().length > 0')) {
    throw new Error('Logout precisa continuar disponível em uma janela estreita.');
  }
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1040, deviceScaleFactor: 1, mobile: false });
  await evaluate('document.querySelector("form input[value=INICIO_INTERVALO]").form.requestSubmit();');
  await waitFor('document.body.innerText.includes("Em intervalo")');
  await evaluate('document.querySelector("form input[value=FIM_INTERVALO]").form.requestSubmit();');
  await waitFor('document.body.innerText.includes("Trabalhando")');
  await navigate('/confirmar-saida');
  await capture('web-confirmacao');
  await evaluate('document.querySelector("form input[value=SAIDA]").form.requestSubmit();');
  await waitFor('document.body.innerText.includes("Encerrada")');
  await navigate('/historico');
  await capture('web-historico-completo');
  await evaluate('document.querySelector("form[action=\\"/logout\\"]").requestSubmit();');
  await waitFor('location.pathname === "/login"');
  await fill({ email, senha: 'TesteVisual123!' });
  await waitFor('location.pathname === "/" && document.body.innerText.includes("Encerrada")');
  if (externas.length) throw new Error('A interface tentou acessar recursos externos: ' + externas.join(', '));
  console.log(JSON.stringify({ ok: true, internetExternaBloqueada: true, screenshots: fs.readdirSync(output).filter(name => name.startsWith('web-')) }));
  socket.close();
})().catch(error => { console.error(error.message); process.exit(1); });
