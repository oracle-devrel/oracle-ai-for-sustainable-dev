const cues = {
  reset: {
    step: "00",
    title: "Begin at a cold start",
    text: "An agent without external memory re-onboards the user every time. Reset proves there is no hidden browser state carrying this demo."
  },
  retain: {
    step: "01",
    title: "Retain the experience, not the whole transcript",
    text: "The write path extracts useful facts, an episode, and a temporary operational note. Each record has an identity scope, type, source, version, and lifecycle."
  },
  recall: {
    step: "02",
    title: "Read before the next turn",
    text: "Recall filters by Ava, the concierge agent, active status, and expiry. It returns a compact context card that the response can reuse."
  },
  correct: {
    step: "03",
    title: "Corrections are first-class data",
    text: "Ava says lantern show, not fireworks. The transaction supersedes the old fact and activates a new version, preserving an auditable history."
  },
  expire: {
    step: "04",
    title: "Not every memory should live forever",
    text: "The rain-route closure is operational memory with a short TTL. Expiring it prevents stale state from influencing tomorrow."
  },
  dream: {
    step: "05",
    title: "Traces become learning material",
    text: "Three successful, privacy-safe traces share a pattern. A fixed demonstration rule induces the same candidate procedure in token space for every presentation; it does not retrain model weights."
  },
  approve: {
    step: "06",
    title: "Learning remains governed",
    text: "A person reviews and approves the proposed skill. Only then can it enter the active procedural-memory path."
  },
  "next-day": {
    step: "07",
    title: "Transfer the lesson, not the private memory",
    text: "Leo benefits from the approved rainy-evening workflow. The scope proof remains zero: none of Ava's private facts are visible."
  }
};

const actionBodies = {
  recall: { guestId: "AVA", query: "Build a rain-safe evening plan" },
  approve: { approver: "demo.presenter@example.com" }
};

const libraryActionBodies = {
  recall: () => ({ query: document.getElementById("library-query").value })
};

let latestContext = null;
let latestNextDay = null;
let previousDatabaseSnapshot = null;
let parkRoute = [];

document.querySelectorAll("[data-action]").forEach(button => {
  button.addEventListener("click", () => runAction(button.dataset.action, button));
});

document.querySelectorAll("[data-library-action]").forEach(button => {
  button.addEventListener("click", () =>
    runLibraryAction(button.dataset.libraryAction, button));
});

document.querySelectorAll("[data-park-action]").forEach(button => {
  button.addEventListener("click", () => runParkAction(button.dataset.parkAction));
});

document.getElementById("refresh-database").addEventListener(
  "click",
  () => refreshDatabaseTables().catch(error =>
    showDatabaseStatus(error.message, true))
);

async function runLibraryAction(action, button) {
  const controls = [...document.querySelectorAll("[data-library-action]")];
  controls.forEach(control => control.disabled = true);
  button.classList.add("active");
  showLibraryResult(
    action === "retain"
      ? "Ollama is extracting records and Oracle AI Database is embedding them…"
      : "Working with Oracle Agent Memory…"
  );
  try {
    const body = libraryActionBodies[action]
      ? libraryActionBodies[action]()
      : {};
    const response = await fetch(`/api/library/actions/${action}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    const value = await response.json();
    if (!response.ok) throw new Error(value.error || `HTTP ${response.status}`);
    renderLibraryState(value);
    showLibraryResult(value.message || "Library step completed.");
    await refreshDatabaseTables().catch(error =>
      showDatabaseStatus(error.message, true));
  } catch (error) {
    showLibraryResult(error.message, true);
  } finally {
    controls.forEach(control => {
      control.disabled = false;
      control.classList.remove("active");
    });
  }
}

async function loadLibraryState() {
  const response = await fetch("/api/library/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "Unable to load library state");
  renderLibraryState(state);
}

function renderLibraryState(state) {
  const messages = state.messages || [];
  const records = state.records || [];
  const results = state.searchResults || [];
  document.getElementById("library-dimensions").textContent =
    state.embeddingDimensions || "-";
  document.getElementById("library-message-count").textContent =
    `${messages.length} message${messages.length === 1 ? "" : "s"}`;
  document.getElementById("library-record-count").textContent =
    `${records.length} record${records.length === 1 ? "" : "s"}`;
  document.getElementById("library-leo-results").textContent =
    state.leoPrivateResults ?? "-";

  document.getElementById("library-messages").innerHTML = messages.length
    ? messages.map(message => `
        <article class="chat ${escapeHtml(message.role)}">
          <b>${escapeHtml(message.role)}</b>
          <p>${escapeHtml(message.content)}</p>
        </article>`).join("")
    : '<p class="empty">No messages yet.</p>';

  document.getElementById("library-records").innerHTML = records.length
    ? records.map(record => `
        <article class="library-record">
          <span class="tag">${escapeHtml(record.type)}</span>
          <p>${escapeHtml(record.content)}</p>
          <small>${escapeHtml(record.userId)} / ${escapeHtml(record.threadId)}</small>
        </article>`).join("")
    : '<p class="empty">No records yet.</p>';

  document.getElementById("library-search").innerHTML = results.length
    ? results.map(result => `
        <article class="rank-row">
          <strong>${escapeHtml(result.rank)}</strong>
          <div><span class="tag">${escapeHtml(result.type)}</span>
          <p>${escapeHtml(result.content)}</p></div>
          <code>${Number(result.distance).toFixed(3)}</code>
        </article>`).join("")
    : '<p class="empty">Run recall to see vector-distance ranking.</p>';

  document.getElementById("library-context").textContent =
    state.contextCard || "No context card yet.";
}

function showLibraryResult(message, error = false) {
  const root = document.getElementById("library-result");
  root.textContent = message;
  root.classList.toggle("error", error);
}

async function runParkAction(action) {
  const controls = [...document.querySelectorAll("[data-park-action]")];
  controls.forEach(control => control.disabled = true);
  showParkResult("Working with Oracle AI Database graph, spatial, vector, and transactional services…");
  try {
    const response = await fetch(`/api/park/actions/${action}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(action === "graphrag"
        ? { query: document.getElementById("park-query").value } : {})
    });
    const state = await response.json();
    if (!response.ok) throw new Error(state.error || `HTTP ${response.status}`);
    if (action === "reset") parkRoute = [];
    if (state.route?.placeIds) parkRoute = state.route.placeIds;
    renderParkState(state);
    showParkResult(state.message || "Memory Quest action completed.");
    await refreshDatabaseTables().catch(error => showDatabaseStatus(error.message, true));
  } catch (error) {
    showParkResult(error.message, true);
  } finally {
    controls.forEach(control => control.disabled = false);
  }
}

async function loadParkState() {
  const response = await fetch("/api/park/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "Unable to load Memory Quest state");
  renderParkState(state);
}

function renderParkState(state) {
  const progress = (state.progress || [])[0];
  const steps = state.quest || [];
  const completed = Number(progress?.CURRENT_STEP || 0);
  document.getElementById("quest-points").textContent = progress?.POINTS_EARNED || 0;
  document.getElementById("quest-badge").textContent =
    state.badges?.[0]?.BADGE_NAME || "No badge yet";
  const status = document.getElementById("quest-status");
  status.textContent = progress?.STATUS || "NOT STARTED";
  status.className = `status-pill ${(progress?.STATUS || "").toLowerCase()}`;

  document.getElementById("park-party").innerHTML = (state.party || []).map(member => `
    <article class="party-member"><span>${escapeHtml(member.DISPLAY_NAME?.slice(0, 1))}</span>
      <div><b>${escapeHtml(member.DISPLAY_NAME)}</b><small>${escapeHtml(member.PARTY_NAME)} · consent until ${shortTime(member.CONSENT_UNTIL)}</small></div>
    </article>`).join("");

  document.getElementById("quest-steps").innerHTML = steps.map(step => {
    const order = Number(step.STEP_ORDER);
    const stepState = order <= completed ? "done" : order === completed + 1 && progress ? "current" : "";
    return `<li class="${stepState}"><b>${order}. ${escapeHtml(step.PLACE_NAME)}</b><span>${escapeHtml(step.CLUE_TEXT)}</span></li>`;
  }).join("");

  document.getElementById("quest-audit").innerHTML = (state.audit || []).length
    ? state.audit.slice().reverse().map(event => `<article><b>${escapeHtml(event.EVENT_TYPE)}</b><span>${Number(event.POINTS_DELTA) >= 0 ? "+" : ""}${escapeHtml(event.POINTS_DELTA)} points</span><small>${escapeHtml(event.DETAILS)}</small></article>`).join("")
    : '<p class="empty">No reward events yet.</p>';

  renderParkMap(state.places || [], state.paths || [], steps, completed);
  if (state.route) {
    document.getElementById("route-explanation").textContent =
      `${state.route.explanation} Graph distance: ${state.route.distanceMeters} m. Spatial straight-line distance: ${Number(state.route.spatialStraightLineMeters).toFixed(1)} m.`;
  }
  if (state.graphRag) renderGraphRag(state.graphRag);
}

function renderParkMap(places, paths, steps, completed) {
  const svg = document.getElementById("park-map");
  const byId = new Map(places.map(place => [place.PLACE_ID, place]));
  const checkpointById = new Map(steps.map(step => [step.PLACE_ID, Number(step.STEP_ORDER)]));
  const uniquePaths = paths.filter(path => String(path.FROM_PLACE_ID) < String(path.TO_PLACE_ID));
  const routeEdges = new Set(parkRoute.slice(1).map((id, index) =>
    [parkRoute[index], id].sort().join("|")));
  const edgeMarkup = uniquePaths.map(path => {
    const from = byId.get(path.FROM_PLACE_ID); const to = byId.get(path.TO_PLACE_ID);
    if (!from || !to) return "";
    const key = [path.FROM_PLACE_ID, path.TO_PLACE_ID].sort().join("|");
    const classes = [Number(path.COVERED) ? "covered" : "", Number(path.ACCESSIBLE) ? "accessible" : "inaccessible", routeEdges.has(key) ? "planned" : ""].filter(Boolean).join(" ");
    return `<line class="park-path ${classes}" x1="${from.X_M}" y1="${720 - from.Y_M}" x2="${to.X_M}" y2="${720 - to.Y_M}"><title>${escapeHtml(path.PATH_NAME)} · ${path.DISTANCE_M} m</title></line>`;
  }).join("");
  const nodeMarkup = places.map(place => {
    const step = checkpointById.get(place.PLACE_ID);
    const nodeState = step && step <= completed ? "done" : step === completed + 1 ? "current" : "";
    return `<g class="park-node ${step ? "checkpoint" : ""} ${nodeState}" transform="translate(${place.X_M} ${720 - place.Y_M})">
      <circle r="${step ? 19 : 13}"></circle>${step ? `<text class="step" text-anchor="middle" y="5">${step}</text>` : ""}
      <text class="label" text-anchor="middle" y="36">${escapeHtml(place.PLACE_NAME)}</text>
      <title>${escapeHtml(place.LORE_SUMMARY)} · ${Number(place.ACCESSIBLE) ? "Accessible" : "Not accessible"}${Number(place.COVERED) ? " · Covered" : ""}</title>
    </g>`;
  }).join("");
  svg.innerHTML = `<defs><pattern id="map-grid" width="40" height="40" patternUnits="userSpaceOnUse"><path d="M40 0H0V40" fill="none" stroke="#dcd8ce" stroke-width="1"/></pattern></defs><rect width="760" height="720" rx="18" fill="url(#map-grid)"/>${edgeMarkup}${nodeMarkup}`;
}

function renderGraphRag(graphRag) {
  document.getElementById("graphrag-answer").textContent = graphRag.answer;
  document.getElementById("graphrag-hits").innerHTML = (graphRag.hits || []).map((hit, index) => `
    <article>
      <header><strong>${index + 1}</strong><div><b>${escapeHtml(hit.title)}</b><small>${escapeHtml(hit.placeId)} · cosine distance ${Number(hit.distance).toFixed(3)}</small></div></header>
      <p>${escapeHtml(hit.content)}</p>
      <div class="graph-expansion"><b>Graph expansion</b>${(hit.graphNeighbors || []).map(node => `<span>${escapeHtml(node.PLACE_NAME)} via ${escapeHtml(node.RELATIONSHIP)}</span>`).join("")}${(hit.quests || []).map(quest => `<span>${escapeHtml(quest.QUEST_NAME)} · step ${escapeHtml(quest.STEP_ORDER)}</span>`).join("")}</div>
    </article>`).join("");
}

function showParkResult(message, error = false) {
  const root = document.getElementById("park-result");
  root.textContent = message; root.classList.toggle("error", error);
}

async function runAction(action, button) {
  setActive(button, action);
  const controls = [...document.querySelectorAll("[data-action]")];
  controls.forEach(control => control.disabled = true);
  showResult("Working with Oracle AI Database…");
  try {
    const response = await fetch(`/api/actions/${action}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(actionBodies[action] || {})
    });
    const value = await response.json();
    if (!response.ok) throw new Error(value.error || `HTTP ${response.status}`);
    if (action === "reset") {
      latestContext = null;
      latestNextDay = null;
    }
    if (action === "recall") latestContext = value.contextCard;
    if (action === "next-day") latestNextDay = value;
    showResult(value.message || "Step completed.");
    await loadState();
    await refreshDatabaseTables().catch(error =>
      showDatabaseStatus(error.message, true));
  } catch (error) {
    showResult(error.message, true);
  } finally {
    controls.forEach(control => control.disabled = false);
  }
}

function setActive(button, action) {
  document.querySelectorAll("[data-action]").forEach(value => value.classList.remove("active"));
  button.classList.add("active");
  applyCue(action);
}

function applyCue(action) {
  const cue = cues[action];
  document.getElementById("step-number").textContent = cue.step;
  document.getElementById("cue-title").textContent = cue.title;
  document.getElementById("cue-text").textContent = cue.text;
}

async function loadState() {
  const response = await fetch("/api/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "Unable to load demo state");
  renderMemories(state.memories || []);
  renderTraces(state.traces || []);
  renderSkills(state.skills || []);
  if (state.latestStage) applyCue(state.latestStage);
  if (state.nextDay) latestNextDay = state.nextDay;
  const activeMemories = (state.memories || []).filter(memory =>
    memory.status === "ACTIVE" &&
    (!memory.expiresAt || new Date(memory.expiresAt) > new Date())
  );
  if (activeMemories.length) {
    latestContext = {
      summary: "Returning guest who values quiet mornings and accessible routes.",
      relevantMemories: activeMemories.map(memory => ({
        type: memory.type,
        content: memory.content
      })),
      proposedResponse:
        "Use a quiet early meal and the covered accessible route, then use the confirmed lantern-show preference."
    };
  }
  renderContext();
  renderScopeProof(state.skills || []);
  if (state.latestStage === "next-day") {
    showResult(state.nextDay.message);
  }
}

function renderMemories(memories) {
  const root = document.getElementById("memory-ledger");
  document.getElementById("memory-count").textContent =
    `${memories.length} record${memories.length === 1 ? "" : "s"}`;
  if (!memories.length) {
    root.innerHTML = '<p class="empty">The durable ledger is empty.</p>';
    return;
  }
  root.innerHTML = memories.map(memory => `
    <article class="memory-row">
      <header>
        <span class="tag">${escapeHtml(memory.type)}</span>
        <span class="tag ${memory.status.toLowerCase()}">${escapeHtml(memory.status)}</span>
        <b>${escapeHtml(memory.key)} · v${memory.version}</b>
      </header>
      <p>${escapeHtml(memory.content)}</p>
      <div class="memory-meta">${escapeHtml(memory.guestId)} / ${escapeHtml(memory.scope)} · ${escapeHtml(memory.source)}${memory.expiresAt ? ` · expires ${shortTime(memory.expiresAt)}` : ""}</div>
    </article>
  `).join("");
}

function renderTraces(traces) {
  const root = document.getElementById("trace-list");
  document.getElementById("trace-count").textContent =
    `${traces.length} trace${traces.length === 1 ? "" : "s"}`;
  if (!traces.length) {
    root.innerHTML = '<p class="empty">No traces yet.</p>';
    return;
  }
  root.innerHTML = traces.slice().reverse().map(trace => `
    <article class="trace">
      <b>${escapeHtml(trace.asked)}</b>
      <small>${escapeHtml(trace.action)} → ${escapeHtml(trace.outcome)}</small>
      ${trace.pattern ? `<small>Pattern: ${escapeHtml(trace.pattern)}</small>` : ""}
    </article>
  `).join("");
}

function renderSkills(skills) {
  const skill = skills.at(-1);
  const status = document.getElementById("skill-status");
  const card = document.getElementById("skill-card");
  if (!skill) {
    status.textContent = "NONE";
    status.className = "status-pill";
    card.textContent = "No skill has been proposed.";
    return;
  }
  status.textContent = skill.status;
  status.className = `status-pill ${skill.status.toLowerCase()}`;
  let instructions = skill.instructions;
  try { instructions = JSON.stringify(JSON.parse(instructions), null, 2); } catch (_) {}
  card.textContent =
    `name: ${skill.name}\nstatus: ${skill.status}\nsource episodes: ${skill.sourceEpisodes}\ntrigger: ${skill.trigger}\n\n${instructions}`;
}

function renderContext() {
  const list = document.getElementById("context-memories");
  if (!latestContext) {
    document.getElementById("context-summary").textContent = "No recall has run yet.";
    document.getElementById("proposed-response").textContent =
      "The response is assembled only after the read step.";
    list.innerHTML = '<li class="empty">Active, unexpired, correctly scoped memories will appear here.</li>';
    return;
  }
  document.getElementById("context-summary").textContent = latestContext.summary;
  document.getElementById("proposed-response").textContent = latestContext.proposedResponse;
  list.innerHTML = latestContext.relevantMemories.map(memory =>
    `<li><b>${escapeHtml(memory.type)}</b> · ${escapeHtml(memory.content)}</li>`
  ).join("");
}

function renderScopeProof(skills) {
  const approved = skills.find(skill => skill.status === "APPROVED");
  document.getElementById("leak-count").textContent =
    latestNextDay ? latestNextDay.privateAvaMemoriesVisible : "-";
  document.getElementById("shared-skill").textContent = approved
    ? `Shared and approved: ${approved.name}. Its instructions contain no private guest data.`
    : "No approved shared skill yet.";
}

function showResult(message, error = false) {
  const root = document.getElementById("result");
  root.textContent = message;
  root.classList.toggle("error", error);
}

async function refreshDatabaseTables({ baseline = false } = {}) {
  const button = document.getElementById("refresh-database");
  button.disabled = true;
  button.textContent = "Refreshing…";
  try {
    const response = await fetch("/api/database/tables");
    const snapshot = await response.json();
    if (!response.ok) {
      throw new Error(snapshot.error || "Unable to inspect database tables");
    }
    const changes = renderDatabaseTables(
      snapshot,
      baseline ? null : previousDatabaseSnapshot
    );
    previousDatabaseSnapshot = snapshot;
    const refreshed = new Date(snapshot.refreshedAt).toLocaleTimeString();
    showDatabaseStatus(
      baseline
        ? `Baseline loaded at ${refreshed}. Run an action or refresh again to highlight changes.`
        : `${changes.added} added, ${changes.changed} changed, and ${changes.removed} removed since the previous refresh · ${refreshed}`
    );
  } finally {
    button.disabled = false;
    button.textContent = "Refresh table contents";
  }
}

function renderDatabaseTables(snapshot, previousSnapshot) {
  const previousByName = new Map(
    (previousSnapshot?.tables || []).map(table => [table.name, table])
  );
  const totals = { added: 0, changed: 0, removed: 0 };
  const root = document.getElementById("database-tables");
  root.innerHTML = (snapshot.tables || []).map(table => {
    const previous = previousByName.get(table.name);
    const previousRows = new Map(
      (previous?.rows || []).map(row => [databaseRowKey(table, row), row])
    );
    const currentKeys = new Set();
    const displayRows = (table.rows || []).map(row => {
      const key = databaseRowKey(table, row);
      currentKeys.add(key);
      const oldRow = previousRows.get(key);
      let change = "";
      if (previous && !oldRow) {
        change = "row-added";
        totals.added++;
      } else if (oldRow && JSON.stringify(oldRow) !== JSON.stringify(row)) {
        change = "row-changed";
        totals.changed++;
      }
      return { row, change };
    });
    if (previous) {
      previousRows.forEach((row, key) => {
        if (!currentKeys.has(key)) {
          displayRows.push({ row, change: "row-removed" });
          totals.removed++;
        }
      });
    }
    const rows = displayRows.length
      ? displayRows.map(({ row, change }) => `
          <tr class="${change}">
            ${table.columns.map(column =>
              `<td>${formatDatabaseValue(row[column])}</td>`
            ).join("")}
          </tr>`).join("")
      : `<tr><td class="database-empty" colspan="${table.columns.length}">
           No rows
         </td></tr>`;
    return `
      <details class="database-table-card" open>
        <summary>
          <span><b>${escapeHtml(table.name)}</b>${escapeHtml(table.description)}</span>
          <strong>${table.rows.length} row${table.rows.length === 1 ? "" : "s"}</strong>
        </summary>
        <div class="database-table-scroll">
          <table>
            <thead><tr>${table.columns.map(column =>
              `<th>${escapeHtml(column)}</th>`
            ).join("")}</tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      </details>`;
  }).join("");
  return totals;
}

function databaseRowKey(table, row) {
  const value = row[table.keyColumn];
  return value == null ? JSON.stringify(row) : String(value);
}

function formatDatabaseValue(value) {
  if (value == null) return '<span class="database-null">NULL</span>';
  const text = typeof value === "object" ? JSON.stringify(value) : String(value);
  return `<span title="${escapeHtml(text)}">${escapeHtml(text)}</span>`;
}

function showDatabaseStatus(message, error = false) {
  const root = document.getElementById("database-refresh-status");
  root.textContent = message;
  root.classList.toggle("error", error);
}

function shortTime(value) {
  try { return new Date(value).toLocaleString(); } catch (_) { return value; }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function initialize() {
  const status = document.getElementById("db-status");
  try {
    const response = await fetch("/api/health");
    const health = await response.json();
    if (!response.ok) throw new Error(health.error);
    status.classList.add("up");
    status.querySelector("span").textContent =
      `${health.schema}@${health.database} · ${health.pool}`;
    await Promise.all([loadState(), loadLibraryState(), loadParkState()]);
    await refreshDatabaseTables({ baseline: true });
  } catch (error) {
    status.classList.add("down");
    status.querySelector("span").textContent = error.message || "Database unavailable";
    showResult(error.message || "Database unavailable", true);
  }
}

initialize();
