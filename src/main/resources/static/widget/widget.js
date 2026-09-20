/* DocPilot embeddable chat widget — dependency-free, ~2 lines to embed.
 *
 * <script src="http://localhost:8080/widget/widget.js"
 *         data-api-url="http://localhost:8080"
 *         data-title="ParcelPilot Support"></script>
 *
 * Optional: window.DocPilotConfig = { apiUrl, title } before the script tag.
 */
(function () {
  'use strict';

  var scriptTag = document.currentScript;
  var cfg = window.DocPilotConfig || {};
  var apiAttr = (scriptTag && scriptTag.getAttribute('data-api-url')) || '';
  var origin = (window.location.origin && window.location.origin !== 'null') ? window.location.origin : '';
  // Precedence: explicit JS config > data-api-url > page origin > localhost default.
  // Omitting data-api-url makes the widget talk to the host serving the page.
  var API_URL = cfg.apiUrl || apiAttr || origin || 'http://localhost:8080';
  var TITLE = cfg.title || (scriptTag && scriptTag.getAttribute('data-title')) || 'Support chat';
  var SESSION_KEY = 'docpilot-session-id';

  function sessionId() {
    var id = null;
    try { id = localStorage.getItem(SESSION_KEY); } catch (e) { /* private mode */ }
    if (!id) {
      id = 'sess-' + Math.random().toString(36).slice(2) + Date.now().toString(36);
      try { localStorage.setItem(SESSION_KEY, id); } catch (e) { /* ignore */ }
    }
    return id;
  }

  var css =
    '.dp-launcher{position:fixed;bottom:24px;right:24px;width:60px;height:60px;border-radius:50%;' +
    'background:#1d4ed8;color:#fff;border:none;cursor:pointer;font-size:26px;z-index:999998;' +
    'box-shadow:0 8px 24px rgba(0,0,0,.25)}' +
    '.dp-panel{position:fixed;bottom:96px;right:24px;width:380px;max-width:calc(100vw - 48px);height:520px;' +
    'max-height:calc(100vh - 140px);background:#fff;border-radius:16px;z-index:999999;display:flex;' +
    'flex-direction:column;overflow:hidden;box-shadow:0 16px 48px rgba(0,0,0,.28);font-family:' +
    '-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif}' +
    '.dp-hidden{display:none!important}' +
    '.dp-header{background:#1d4ed8;color:#fff;padding:14px 16px;display:flex;' +
    'justify-content:space-between;align-items:center}' +
    '.dp-header b{font-size:15px}.dp-header small{display:block;font-weight:400;opacity:.85;font-size:12px}' +
    '.dp-close{background:none;border:none;color:#fff;font-size:20px;cursor:pointer}' +
    '.dp-msgs{flex:1;overflow-y:auto;padding:14px;display:flex;flex-direction:column;gap:10px;background:#f8fafc}' +
    '.dp-msg{max-width:85%;padding:10px 12px;border-radius:12px;font-size:14px;line-height:1.45;white-space:pre-wrap}' +
    '.dp-bot{background:#fff;border:1px solid #e2e8f0;align-self:flex-start;border-bottom-left-radius:4px}' +
    '.dp-user{background:#1d4ed8;color:#fff;align-self:flex-end;border-bottom-right-radius:4px}' +
    '.dp-cite{font-size:11px;color:#64748b;margin-top:6px;border-top:1px dashed #e2e8f0;padding-top:6px}' +
    '.dp-cite div{margin-top:2px}' +
    '.dp-fb{margin-top:6px;font-size:12px;color:#94a3b8}' +
    '.dp-fb button{background:none;border:1px solid #e2e8f0;border-radius:8px;cursor:pointer;' +
    'font-size:13px;padding:2px 8px;margin-right:6px}' +
    '.dp-fb button.dp-on{background:#dcfce7;border-color:#86efac}' +
    '.dp-ticket{margin-top:8px;background:#fefce8;border:1px solid #fde68a;border-radius:10px;padding:10px}' +
    '.dp-ticket input{width:100%;box-sizing:border-box;margin:4px 0;padding:8px;border:1px solid #e2e8f0;border-radius:8px;font-size:13px}' +
    '.dp-ticket button{background:#1d4ed8;color:#fff;border:none;border-radius:8px;padding:8px 14px;' +
    'font-size:13px;cursor:pointer;margin-top:4px}' +
    '.dp-input{display:flex;border-top:1px solid #e2e8f0;padding:10px;gap:8px;background:#fff}' +
    '.dp-input input{flex:1;border:1px solid #e2e8f0;border-radius:10px;padding:10px;font-size:14px}' +
    '.dp-input button{background:#1d4ed8;color:#fff;border:none;border-radius:10px;padding:0 16px;cursor:pointer;font-size:14px}' +
    '.dp-typing{font-size:13px;color:#94a3b8;font-style:italic}';

  var style = document.createElement('style');
  style.textContent = css;
  document.head.appendChild(style);

  var launcher = document.createElement('button');
  launcher.className = 'dp-launcher';
  launcher.innerHTML = '&#128172;';
  launcher.setAttribute('aria-label', 'Open support chat');
  document.body.appendChild(launcher);

  var panel = document.createElement('div');
  panel.className = 'dp-panel dp-hidden';
  panel.innerHTML =
    '<div class="dp-header"><div><b></b><small>Answers from our help center, with sources</small></div>' +
    '<button class="dp-close" aria-label="Close">&times;</button></div>' +
    '<div class="dp-msgs"></div>' +
    '<div class="dp-input"><input type="text" placeholder="Ask a question…" aria-label="Ask a question">' +
    '<button>Send</button></div>';
  panel.querySelector('.dp-header b').textContent = TITLE;
  document.body.appendChild(panel);

  var msgs = panel.querySelector('.dp-msgs');
  var input = panel.querySelector('.dp-input input');
  var sendBtn = panel.querySelector('.dp-input button');
  var open = false;

  function toggle(force) {
    open = typeof force === 'boolean' ? force : !open;
    panel.classList.toggle('dp-hidden', !open);
    if (open && !msgs.children.length) {
      botSay('Hi! Ask me anything about our help center — I answer from our docs and show my sources.');
    }
    if (open) { input.focus(); }
  }
  launcher.addEventListener('click', function () { toggle(); });
  panel.querySelector('.dp-close').addEventListener('click', function () { toggle(false); });

  function scroll() { msgs.scrollTop = msgs.scrollHeight; }

  function addMsg(text, who) {
    var d = document.createElement('div');
    d.className = 'dp-msg dp-' + who;
    d.textContent = text;
    msgs.appendChild(d);
    scroll();
    return d;
  }

  function botSay(text) { return addMsg(text, 'bot'); }

  function post(path, body) {
    return fetch(API_URL + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }).then(function (r) {
      if (!r.ok) { throw new Error('HTTP ' + r.status); }
      if (r.status === 204) { return null; }
      return r.json();
    });
  }

  function renderCitations(el, citations) {
    if (!citations || !citations.length) { return; }
    var c = document.createElement('div');
    c.className = 'dp-cite';
    c.textContent = 'Sources:';
    citations.forEach(function (ct) {
      var d = document.createElement('div');
      d.textContent = '[' + ct.index + '] ' + ct.source + ' — ' + ct.excerpt;
      c.appendChild(d);
    });
    el.appendChild(c);
  }

  function renderFeedback(el, interactionId) {
    var f = document.createElement('div');
    f.className = 'dp-fb';
    f.textContent = 'Was this helpful? ';
    var up = document.createElement('button'); up.textContent = '👍';
    var down = document.createElement('button'); down.textContent = '👎';
    function vote(v, btn) {
      post('/api/v1/chat/feedback', { interactionId: interactionId, helpful: v })
        .then(function () {
          up.classList.toggle('dp-on', v === true);
          down.classList.toggle('dp-on', v === false);
        })
        .catch(function () { /* feedback is best-effort */ });
      btn.blur();
    }
    up.addEventListener('click', function () { vote(true, up); });
    down.addEventListener('click', function () { vote(false, down); });
    f.appendChild(up); f.appendChild(down);
    el.appendChild(f);
  }

  function renderTicketOffer(el, draft) {
    var box = document.createElement('div');
    box.className = 'dp-ticket';
    box.innerHTML = '<b>Open a support ticket</b><br><small>Our team will follow up by email.</small>';
    var name = document.createElement('input'); name.placeholder = 'Your name';
    var email = document.createElement('input'); email.placeholder = 'Email'; email.type = 'email';
    var btn = document.createElement('button'); btn.textContent = 'Create ticket';
    btn.addEventListener('click', function () {
      btn.disabled = true; btn.textContent = 'Creating…';
      post('/api/v1/tickets', {
        subject: draft.subject, description: draft.description,
        requesterName: name.value || null, requesterEmail: email.value || null
      }).then(function (t) {
        box.innerHTML = '✅ Ticket created — <b>' + t.id.slice(0, 8) + '</b>. We\'ll be in touch!';
      }).catch(function () {
        btn.disabled = false; btn.textContent = 'Create ticket';
        box.insertAdjacentHTML('beforeend', '<div style="color:#b91c1c;font-size:12px">Could not create the ticket. Please try again.</div>');
      });
    });
    box.appendChild(name); box.appendChild(email); box.appendChild(btn);
    el.appendChild(box);
  }

  function send() {
    var q = input.value.trim();
    if (!q) { return; }
    input.value = '';
    addMsg(q, 'user');
    var typing = document.createElement('div');
    typing.className = 'dp-typing';
    typing.textContent = 'Thinking…';
    msgs.appendChild(typing); scroll();

    post('/api/v1/chat', { sessionId: sessionId(), question: q })
      .then(function (res) {
        typing.remove();
        var el = botSay(res.answer);
        renderCitations(el, res.citations);
        if (res.escalated && res.ticketDraft) {
          renderTicketOffer(el, res.ticketDraft);
        } else if (res.interactionId) {
          renderFeedback(el, res.interactionId);
        }
      })
      .catch(function () {
        typing.remove();
        botSay('Sorry — I couldn\'t reach the support service. Please try again in a moment.');
      });
  }

  sendBtn.addEventListener('click', send);
  input.addEventListener('keydown', function (e) { if (e.key === 'Enter') { send(); } });
})();
