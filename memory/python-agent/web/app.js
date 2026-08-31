const titles = {
  reset: "Cold start confirmed",
  retain: "Memory retained through the SDK",
  recall: "Exact scoped recall completed",
  correct: "Durable fact corrected",
  expire: "TTL removed stale context",
  dream: "Trace pattern became a guideline",
  approve: "Human approval activated learning",
  "next-day": "Shared skill reused without leakage"
};

let lastNextDay = null;
let parkRoute = [];
let previousDatabaseSnapshot = null;
let arSession = null;

document.querySelectorAll("[data-action]").forEach(button => {
  button.addEventListener("click", () => runAction(button.dataset.action, button));
});

document.querySelectorAll("[data-park-action]").forEach(button => {
  button.addEventListener("click", () => runParkAction(button.dataset.parkAction));
});

document.getElementById("refresh-database").addEventListener("click", () =>
  refreshDatabaseTables().catch(error => showDatabaseStatus(error.message)));
document.getElementById("refresh-security").addEventListener("click", () =>
  loadSecurityState().catch(error => renderSecurityError(error)));

document.getElementById("ar-recording").addEventListener("change", updateArPrivacyBeacon);
document.getElementById("ar-start").addEventListener("click", startArSession);
document.getElementById("ar-reset").addEventListener("click", resetArSession);
document.getElementById("ar-remember").addEventListener("click", () => arRequest("remember", {
  text: document.getElementById("ar-memory-text").value,
  source: "browser"
}));
document.getElementById("ar-index-media").addEventListener("click", () => arRequest("media", {
  transcript: document.getElementById("ar-media-text").value,
  mediaType: "video-transcript"
}));
document.getElementById("ar-search").addEventListener("click", () => arRequest("search", {
  query: document.getElementById("ar-search-text").value
}));

async function runAction(action, button) {
  const buttons = [...document.querySelectorAll("[data-action]")];
  buttons.forEach(value => {
    value.disabled = true;
    value.classList.toggle("active", value === button);
  });
  setResult("Calling Oracle AI Agent Memory...", "The database transaction is in progress.");
  try {
    const response = await fetch(`/api/actions/${action}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}"
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || `HTTP ${response.status}`);
    if (action === "next-day") lastNextDay = result;
    if (action === "reset") lastNextDay = null;
    setResult(titles[action], result.message);
    if (result.contextCard) renderContext(result.contextCard);
    await loadState();
    await Promise.all([refreshDatabaseTables(), loadSecurityState()]);
  } catch (error) {
    setResult("Action failed", error.message);
  } finally {
    buttons.forEach(value => value.disabled = false);
  }
}

async function loadSecurityState() {
  const response = await fetch("/api/security/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "Security proof unavailable");
  const status = document.getElementById("security-status");
  status.className = `security-status ${state.databaseEnforced ? "enforced" : "setup-required"}`;
  status.textContent = state.message;
  const identities = document.getElementById("security-identities");
  identities.innerHTML = (state.identities || []).map(identity => {
    const rows = (identity.rows || []).map(row => `
      <li><b>${escapeHtml(row.RECORD_ID)}</b><span>${escapeHtml(row.MEMORY_TYPE)} · ${escapeHtml(row.USER_ID || "SHARED")}</span></li>
    `).join("") || "<li><span>No rows visible.</span></li>";
    const isolation = identity.id === "LEO"
      ? `<strong>${identity.avaPrivateRows} Ava-private rows returned</strong>`
      : `<strong>${identity.privateRows} own private rows returned</strong>`;
    return `<article class="security-identity">
      <header><span>${escapeHtml(identity.id)}</span><div><b>${escapeHtml(identity.role)}</b><small>${escapeHtml(identity.dataRole)}</small></div></header>
      <p>${isolation}<span>${identity.rowCount} total authorized rows</span></p>
      <ul>${rows}</ul>
    </article>`;
  }).join("") || `<article class="security-identity"><b>Setup required</b><p>${escapeHtml(state.detail || "Run the DDS bootstrap.")}</p></article>`;
  const probe = state.crossUserProbe;
  const probeRoot = document.getElementById("security-probe");
  if (probe) {
    probeRoot.className = `security-probe ${probe.blocked ? "blocked" : "warning"}`;
    probeRoot.innerHTML = `<b>${probe.blocked ? "BLOCKED IN DATABASE" : "REVIEW REQUIRED"}</b><code>${escapeHtml(probe.sql)}</code><span>Executed as ${escapeHtml(probe.executedAs)} · ${probe.returnedRows} rows returned</span>`;
  } else {
    probeRoot.className = "security-probe warning";
    probeRoot.innerHTML = "<b>NOT YET ENFORCED</b><span>Run the bootstrap, then restart the app.</span>";
  }
}

function renderSecurityError(error) {
  const status = document.getElementById("security-status");
  status.className = "security-status setup-required";
  status.textContent = error.message;
}

function setResult(title, text) {
  document.getElementById("result-title").textContent = title;
  document.getElementById("result-text").textContent = text;
}

async function loadState() {
  const response = await fetch("/api/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "State unavailable");
  renderRecords(state.memories || []);
  renderTraces(state.traces || []);
  renderSkill(state.skills || []);
  renderEvents(state.events || []);
  if (state.contextCard) renderContext(state.contextCard);
  if (state.nextDay) lastNextDay = state.nextDay;
  if (state.latestStage) {
    setResult(titles[state.latestStage.stage] || state.latestStage.stage, state.latestStage.detail);
  }
  document.getElementById("memory-count").textContent = state.memories.length;
  document.getElementById("trace-count").textContent = state.traces.length;
  document.getElementById("skill-count").textContent = state.skills.length;
  document.getElementById("leak-count").textContent =
    lastNextDay ? lastNextDay.privateAvaMemoriesVisible : "?";
}

function renderContext(context) {
  document.getElementById("context-summary").textContent = context.summary;
  document.getElementById("context-response").textContent = context.response;
  const root = document.getElementById("context-list");
  const memories = context.relevantMemories || [];
  root.innerHTML = memories.length ? memories.map(value => `
    <div class="memory-chip"><b>${escapeHtml(value.recordType)}</b> ${escapeHtml(value.content)}</div>
  `).join("") : '<p class="empty">No eligible memory was recalled.</p>';
}

function renderRecords(records) {
  const root = document.getElementById("memory-list");
  root.innerHTML = records.length ? records.map(value => `
    <article class="record">
      <div class="tags">
        <span class="tag">${escapeHtml(value.recordType)}</span>
        <span class="tag">${escapeHtml(value.metadata.kind || "memory")}</span>
      </div>
      <p>${escapeHtml(value.content)}</p>
      <small>${escapeHtml(value.userId)} / ${escapeHtml(value.agentId)} · ${escapeHtml(value.id)}</small>
    </article>
  `).join("") : '<p class="empty">No Ava memories are currently searchable.</p>';
}

function renderTraces(traces) {
  const root = document.getElementById("trace-list");
  root.innerHTML = traces.length ? traces.map(value => `
    <div class="trace"><b>${escapeHtml(value.metadata.pattern)}</b><br>${escapeHtml(value.content)}</div>
  `).join("") : '<p class="empty">No governed traces.</p>';
}

function renderSkill(skills) {
  const root = document.getElementById("skill-card");
  const status = document.getElementById("skill-status");
  const skill = skills[0];
  if (!skill) {
    root.textContent = "No guideline has been induced.";
    status.textContent = "NONE";
    status.className = "";
    return;
  }
  const metadata = skill.metadata || {};
  status.textContent = (metadata.status || "pending").toUpperCase();
  status.className = metadata.status === "approved" ? "approved" : "";
  root.textContent = [
    `id: ${skill.id}`,
    `type: ${skill.recordType}`,
    `status: ${metadata.status}`,
    `source episodes: ${metadata.source_episode_count}`,
    `private guest data: ${metadata.private_guest_data_included}`,
    "",
    skill.content
  ].join("\n");
}

function renderEvents(events) {
  const root = document.getElementById("event-list");
  root.innerHTML = events.length ? events.slice().reverse().map(value => `
    <article class="event"><b>${escapeHtml(value.stage)}</b><small>${escapeHtml(value.detail)}</small></article>
  `).join("") : '<p class="empty">No actions yet.</p>';
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function runParkAction(action) {
  const controls = [...document.querySelectorAll("[data-park-action]")];
  controls.forEach(control => control.disabled = true);
  try {
    const response = await fetch(`/api/park/actions/${action}`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify(action === "graphrag"
        ? { query: document.getElementById("park-query").value } : {})
    });
    const state = await response.json();
    if (!response.ok) throw new Error(state.error || `HTTP ${response.status}`);
    if (action === "reset") parkRoute = [];
    if (state.route?.placeIds) parkRoute = state.route.placeIds;
    renderParkState(state);
    document.getElementById("park-result").textContent = state.message;
    await refreshDatabaseTables();
  } catch (error) {
    document.getElementById("park-result").textContent = error.message;
  } finally { controls.forEach(control => control.disabled = false); }
}

async function loadParkState() {
  const response = await fetch("/api/park/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "Memory Quest unavailable");
  renderParkState(state);
}

function renderParkState(state) {
  const progress = (state.progress || [])[0];
  const completed = Number(progress?.CURRENT_STEP || 0);
  document.getElementById("quest-points").textContent = progress?.POINTS_EARNED || 0;
  document.getElementById("quest-badge").textContent = state.badges?.[0]?.BADGE_NAME || "No badge yet";
  document.getElementById("quest-status").textContent = progress?.STATUS || "NOT STARTED";
  document.getElementById("park-party").innerHTML = (state.party || []).map(member => `
    <article class="party-member"><span>${escapeHtml(member.DISPLAY_NAME?.slice(0, 1))}</span>
    <div><b>${escapeHtml(member.DISPLAY_NAME)}</b><small>${escapeHtml(member.PARTY_NAME)} · consent until ${escapeHtml(member.CONSENT_UNTIL)}</small></div></article>`).join("");
  document.getElementById("quest-steps").innerHTML = (state.quest || []).map(step => {
    const order = Number(step.STEP_ORDER);
    const css = order <= completed ? "done" : order === completed + 1 && progress ? "current" : "";
    return `<li class="${css}"><b>${order}. ${escapeHtml(step.PLACE_NAME)}</b><span>${escapeHtml(step.CLUE_TEXT)}</span></li>`;
  }).join("");
  document.getElementById("quest-audit").innerHTML = (state.audit || []).slice().reverse().map(event =>
    `<article><b>${escapeHtml(event.EVENT_TYPE)}</b><span>+${escapeHtml(event.POINTS_DELTA)} points</span><small>${escapeHtml(event.DETAILS)}</small></article>`).join("") || '<p class="empty">No reward events yet.</p>';
  renderParkMap(state.places || [], state.paths || [], state.quest || [], completed);
  if (state.route) document.getElementById("route-explanation").textContent =
    `${state.route.explanation} Graph: ${state.route.distanceMeters} m; spatial straight line: ${Number(state.route.spatialStraightLineMeters).toFixed(1)} m.`;
  if (state.graphRag) {
    document.getElementById("graphrag-answer").textContent = state.graphRag.answer;
    document.getElementById("graphrag-hits").innerHTML = state.graphRag.hits.map((hit, index) => `
      <article><b>${index + 1}. ${escapeHtml(hit.title)}</b><small>${escapeHtml(hit.placeId)} · ${Number(hit.distance).toFixed(3)}</small><p>${escapeHtml(hit.content)}</p>
      <div class="graph-expansion">${(hit.graphNeighbors || []).map(node => `<span>${escapeHtml(node.PLACE_NAME)} via ${escapeHtml(node.RELATIONSHIP)}</span>`).join("")}</div></article>`).join("");
  }
}

function renderParkMap(places, paths, steps, completed) {
  const byId = new Map(places.map(place => [place.PLACE_ID, place]));
  const checkpoint = new Map(steps.map(step => [step.PLACE_ID, Number(step.STEP_ORDER)]));
  const routeEdges = new Set(parkRoute.slice(1).map((id, index) => [parkRoute[index], id].sort().join("|")));
  const edges = paths.filter(path => String(path.FROM_PLACE_ID) < String(path.TO_PLACE_ID)).map(path => {
    const from = byId.get(path.FROM_PLACE_ID), to = byId.get(path.TO_PLACE_ID);
    if (!from || !to) return "";
    const key = [path.FROM_PLACE_ID, path.TO_PLACE_ID].sort().join("|");
    const css = `${Number(path.COVERED) ? "covered" : ""} ${Number(path.ACCESSIBLE) ? "" : "inaccessible"} ${routeEdges.has(key) ? "planned" : ""}`;
    return `<line class="park-path ${css}" x1="${from.X_M}" y1="${720-from.Y_M}" x2="${to.X_M}" y2="${720-to.Y_M}"></line>`;
  }).join("");
  const nodes = places.map(place => {
    const step = checkpoint.get(place.PLACE_ID), css = step <= completed ? "done" : step === completed + 1 ? "current" : "";
    return `<g class="park-node ${step ? "checkpoint" : ""} ${css}" transform="translate(${place.X_M} ${720-place.Y_M})"><circle r="${step ? 19 : 13}"></circle>${step ? `<text text-anchor="middle" y="5">${step}</text>` : ""}<text class="label" text-anchor="middle" y="36">${escapeHtml(place.PLACE_NAME)}</text></g>`;
  }).join("");
  document.getElementById("park-map").innerHTML = edges + nodes;
}

async function refreshDatabaseTables({ baseline = false } = {}) {
  const response = await fetch("/api/database/tables");
  const snapshot = await response.json();
  if (!response.ok) throw new Error(snapshot.error || "Database inspection failed");
  renderDatabaseTables(snapshot, baseline ? null : previousDatabaseSnapshot);
  previousDatabaseSnapshot = snapshot;
  showDatabaseStatus(`Refreshed ${new Date(snapshot.refreshedAt).toLocaleTimeString()}`);
}

function renderDatabaseTables(snapshot, previousSnapshot) {
  const oldTables = new Map((previousSnapshot?.tables || []).map(table => [table.name, table]));
  document.getElementById("database-tables").innerHTML = (snapshot.tables || []).map(table => {
    const old = oldTables.get(table.name);
    const oldRows = new Map((old?.rows || []).map(row => [String(row[table.keyColumn]), row]));
    const body = (table.rows || []).map(row => {
      const prior = oldRows.get(String(row[table.keyColumn]));
      const css = old && !prior ? "row-added" : prior && JSON.stringify(prior) !== JSON.stringify(row) ? "row-changed" : "";
      return `<tr class="${css}">${table.columns.map(column => `<td>${escapeHtml(row[column] ?? "NULL")}</td>`).join("")}</tr>`;
    }).join("") || `<tr><td colspan="${table.columns.length}">No rows</td></tr>`;
    return `<details class="database-table-card" open><summary><span><b>${escapeHtml(table.name)}</b>${escapeHtml(table.description)}</span><strong>${table.rows.length} rows</strong></summary><div class="database-table-scroll"><table><thead><tr>${table.columns.map(column => `<th>${escapeHtml(column)}</th>`).join("")}</tr></thead><tbody>${body}</tbody></table></div></details>`;
  }).join("");
}

function showDatabaseStatus(message) { document.getElementById("database-refresh-status").textContent = message; }

function updateArPrivacyBeacon() {
  const recording = document.getElementById("ar-recording").checked;
  const beacon = document.querySelector(".privacy-beacon");
  beacon.classList.toggle("recording", recording);
  document.getElementById("ar-privacy-label").textContent = recording
    ? "Recording consent on" : "Recording off";
}

async function startArSession() {
  try {
    const result = await postAr("session/start", {
      guestId: "AVA",
      cameraSensing: document.getElementById("ar-camera").checked,
      mediaRecording: document.getElementById("ar-recording").checked,
      locationSharing: document.getElementById("ar-location").checked,
      retentionDays: Number(document.getElementById("ar-retention").value)
    }, false);
    arSession = { sessionId: result.sessionId, sessionToken: result.sessionToken };
    document.getElementById("ar-session").textContent = `AVA · ${result.sessionId.slice(0, 11)}`;
    document.getElementById("ar-sensing").textContent = result.privacy.cameraSensing ? "SENSING ON" : "SENSING OFF";
    renderArResult(result);
  } catch (error) { renderArError(error); }
}

async function resetArSession() {
  try {
    const result = await postAr("reset", {}, false);
    arSession = null;
    document.getElementById("ar-session").textContent = "NO ACTIVE SESSION";
    document.getElementById("ar-sensing").textContent = "SENSING OFF";
    renderArResult(result);
  } catch (error) { renderArError(error); }
}

async function arRequest(action, body) {
  if (!arSession) {
    renderArError(new Error("Start an AR session first."));
    return;
  }
  try {
    renderArResult(await postAr(action, body, true));
    await Promise.all([loadState(), refreshDatabaseTables()]);
  } catch (error) { renderArError(error); }
}

async function postAr(action, body, includeSession) {
  const payload = includeSession ? { ...body, ...arSession } : body;
  const response = await fetch(`/api/ar/${action}`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || `HTTP ${response.status}`);
  return result;
}

function renderArResult(result) {
  document.getElementById("ar-overlay").textContent = result.overlay || result.message;
  const resultCard = document.getElementById("ar-result");
  let title = "Action completed";
  let detail = result.message || result.overlay || "The operation completed.";
  if (result.sessionId) {
    title = "Step 1 complete: private session started";
    detail = `Sensing ${result.privacy.cameraSensing ? "on" : "off"}; recording ${result.privacy.mediaRecording ? "on with consent" : "off"}; precise location ${result.privacy.locationSharing ? "shared" : "not shared"}; retention ${result.privacy.retentionDays} days.`;
  } else if (result.memoryId) {
    title = "Step 2 complete: observation remembered";
    detail = "The confirmed observation is now guest-scoped durable memory with bounded retention. Check the memory and AR audit tables below.";
  } else if (result.mediaId) {
    title = "Step 3 complete: media meaning indexed";
    detail = "The consented description received a native database vector and expiration. Raw video was not stored by this action.";
  } else if (result.hits) {
    title = `Step 4 complete: ${result.hits.length} scoped result${result.hits.length === 1 ? "" : "s"} found`;
    detail = result.hits.length
      ? result.hits.map(hit => `${hit.TRANSCRIPT} (distance ${Number(hit.DISTANCE).toFixed(3)})`).join(" ")
      : "No unexpired, opted-in media descriptions matched this visitor and query.";
  } else if (result.message?.includes("reset")) {
    title = "AR evidence reset";
    detail = "The demonstration sessions, media descriptions, audit events, and tracked AR memories were removed.";
  }
  resultCard.replaceChildren();
  const heading = document.createElement("b");
  heading.textContent = title;
  const message = document.createElement("span");
  message.textContent = detail;
  resultCard.append(heading, message);
}

function renderArError(error) {
  document.getElementById("ar-overlay").textContent = error.message;
  const resultCard = document.getElementById("ar-result");
  resultCard.replaceChildren();
  const heading = document.createElement("b");
  heading.textContent = "Action blocked";
  const message = document.createElement("span");
  message.textContent = error.message;
  resultCard.append(heading, message);
}

async function initialize() {
  const status = document.getElementById("status");
  try {
    const response = await fetch("/api/health");
    const health = await response.json();
    if (!response.ok) throw new Error(health.error);
    status.classList.add("up");
    status.querySelector("span").textContent =
      `${health.sdk} · ${health.schema}@${health.database}`;
    document.getElementById("strategy").textContent = health.strategy;
    await Promise.all([loadState(), loadParkState(), loadSecurityState()]);
    await refreshDatabaseTables({ baseline: true });
    updateArPrivacyBeacon();
  } catch (error) {
    status.classList.add("down");
    status.querySelector("span").textContent = error.message;
    setResult("Connection failed", error.message);
  }
}

initialize();
