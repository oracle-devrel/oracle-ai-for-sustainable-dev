const allowedComponents = new Set(["Column", "Row", "List", "Card", "Text", "Button", "TextField"]);
const state = { message: "", approvalId: null, recommendations: [], components: new Map(), data: null };
const statusEl = document.querySelector("#status");
const messageEl = document.querySelector("#assistant-message");
const surfaceEl = document.querySelector("#a2ui-surface");
const eventsEl = document.querySelector("#events");
const runForm = document.querySelector("#run-form");

runForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  reset();
  statusEl.textContent = "Running";
  setBusy(true);
  try {
    const response = await fetch("/api/runs", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams(new FormData(runForm))
    });
    if (!response.ok) throw new Error(await response.text());
    await readSse(response.body);
  } catch (error) {
    showError(error.message);
  } finally {
    setBusy(false);
  }
});

async function readSse(stream) {
  const reader = stream.pipeThrough(new TextDecoderStream()).getReader();
  let buffer = "";
  while (true) {
    const { value, done } = await reader.read();
    buffer += value || "";
    const frames = buffer.split("\n\n");
    buffer = frames.pop();
    for (const frame of frames) {
      const line = frame.split("\n").find(item => item.startsWith("data: "));
      if (line) handleEvent(JSON.parse(line.slice(6)));
    }
    if (done) break;
  }
}

function handleEvent(event) {
  appendEvent(event);
  if (event.type === "TEXT_MESSAGE_CONTENT") {
    state.message += event.delta;
    messageEl.textContent = state.message;
  } else if (event.type === "STATE_SNAPSHOT") {
    state.approvalId = event.snapshot.approvalId;
    statusEl.textContent = humanize(event.snapshot.status);
  } else if (event.type === "CUSTOM" && event.name === "a2ui.message") {
    consumeA2ui(event.value);
  } else if (event.type === "RUN_ERROR") {
    showError(event.message);
  }
}

function consumeA2ui(envelope) {
  validateEnvelope(envelope);
  if (envelope.updateComponents) {
    for (const component of envelope.updateComponents.components) {
      if (!allowedComponents.has(component.component)) {
        throw new Error(`A2UI component not allowed: ${component.component}`);
      }
      state.components.set(component.id, component);
    }
  }
  if (envelope.updateDataModel) {
    state.data = envelope.updateDataModel.value;
    state.recommendations = state.data.recommendations || [];
    state.approvalId = state.data.approvalId;
    renderReview();
  }
}

function validateEnvelope(envelope) {
  if (envelope.version !== "v0.9.1") throw new Error("Unsupported A2UI version");
  const keys = ["createSurface", "updateComponents", "updateDataModel", "deleteSurface"]
    .filter(key => key in envelope);
  if (keys.length !== 1) throw new Error("Invalid A2UI envelope");
  const payload = envelope[keys[0]];
  if (payload.surfaceId !== "inventory-transfer-review") {
    throw new Error("Unexpected A2UI surface");
  }
  if (envelope.createSurface
      && envelope.createSurface.catalogId
        !== "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json") {
    throw new Error("Unexpected A2UI catalog");
  }
}

function renderReview() {
  surfaceEl.replaceChildren();
  const summary = document.createElement("p");
  summary.className = "result";
  summary.textContent = state.data.summary;
  const grid = document.createElement("div");
  grid.className = "recommendation-grid";
  state.recommendations.forEach((recommendation, index) =>
    grid.append(recommendationCard(recommendation, index === 0)));
  surfaceEl.append(summary, grid);
  if (state.recommendations.length > 0) surfaceEl.append(approvalForm());
}

function recommendationCard(recommendation, checked) {
  const label = document.createElement("label");
  label.className = "recommendation";
  const top = document.createElement("span");
  top.className = "recommendation-top";
  const choice = document.createElement("span");
  const radio = document.createElement("input");
  radio.type = "radio";
  radio.name = "recommendationId";
  radio.value = recommendation.recommendationId;
  radio.checked = checked;
  const name = document.createElement("strong");
  name.textContent = ` ${recommendation.sku} · ${recommendation.productName}`;
  choice.append(radio, name);
  const badge = document.createElement("span");
  badge.className = "badge";
  badge.textContent = `${recommendation.riskLevel} · ${recommendation.stockoutRiskScore}`;
  top.append(choice, badge);

  const route = document.createElement("p");
  route.className = "route";
  route.textContent =
    `${recommendation.sourceLocationCode} → ${recommendation.targetLocationCode}`;
  const rationale = document.createElement("p");
  rationale.textContent = recommendation.rationale;
  const details = document.createElement("dl");
  addDetail(details, "Transfer", `${recommendation.recommendedTransferQuantity} units`);
  addDetail(details, "Shortage", `${recommendation.shortageQuantity} units`);
  addDetail(details, "Transit", `${recommendation.transitDays} days`);
  addDetail(
    details,
    "Estimated lane cost",
    new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD"
    }).format(
      recommendation.recommendedTransferQuantity
        * recommendation.unitTransferCost));
  label.append(top, route, rationale, details);
  return label;
}

function addDetail(list, termText, valueText) {
  const box = document.createElement("div");
  const term = document.createElement("dt");
  term.textContent = termText;
  const value = document.createElement("dd");
  value.textContent = valueText;
  box.append(term, value);
  list.append(box);
}

function approvalForm() {
  const form = document.createElement("form");
  form.className = "approval";
  const notesLabel = document.createElement("label");
  notesLabel.textContent = "Transfer approval notes";
  const notes = document.createElement("textarea");
  notes.name = "approvalNotes";
  notes.minLength = 3;
  notes.maxLength = 2000;
  notes.required = true;
  notes.value = state.data.form.approvalNotes;
  notesLabel.append(notes);
  const actions = document.createElement("div");
  actions.className = "approval-actions";
  const approve = document.createElement("button");
  approve.type = "submit";
  approve.textContent = "Approve inventory transfer";
  const reject = document.createElement("button");
  reject.type = "button";
  reject.className = "secondary";
  reject.textContent = "Cancel";
  actions.append(approve, reject);
  form.append(notesLabel, actions);
  form.addEventListener("submit", submitApproval);
  reject.addEventListener("click", rejectApproval);
  return form;
}

async function submitApproval(event) {
  event.preventDefault();
  const selected = document.querySelector(
    'input[name="recommendationId"]:checked');
  if (!selected) return showError("Select a transfer recommendation first.");
  const form = new FormData(event.currentTarget);
  form.set("recommendationId", selected.value);
  form.set("approvalId", state.approvalId);
  await postAction(
    "/api/approve",
    form,
    result =>
      `Transfer ${result.transferId} is ${result.status}: `
        + `${result.transferQuantity} units reserved for movement.`);
}

async function rejectApproval() {
  const form = new FormData();
  form.set("approvalId", state.approvalId);
  await postAction(
    "/api/reject",
    form,
    () => "Transfer rejected. No inventory was reserved and no transfer was created.");
}

async function postAction(url, form, message) {
  setBusy(true);
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams(form)
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || "Action failed");
    statusEl.textContent = humanize(result.status);
    surfaceEl.replaceChildren(Object.assign(
      document.createElement("p"),
      { className: "result", textContent: message(result) }));
  } catch (error) {
    showError(error.message);
  } finally {
    setBusy(false);
  }
}

function appendEvent(event) {
  const item = document.createElement("li");
  if (event.type.startsWith("TOOL_CALL")) item.className = "tool";
  item.textContent = event.type
    + (event.toolCallName
      ? ` · ${event.toolCallName}`
      : event.stepName
        ? ` · ${event.stepName}`
        : "");
  eventsEl.append(item);
  item.scrollIntoView({ block: "nearest" });
}

function reset() {
  state.message = "";
  state.approvalId = null;
  state.recommendations = [];
  state.components.clear();
  state.data = null;
  eventsEl.replaceChildren();
  surfaceEl.replaceChildren();
  messageEl.textContent = "";
}

function setBusy(busy) {
  document.querySelectorAll("button")
    .forEach(button => button.disabled = busy);
}

function humanize(value) {
  return String(value)
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, character => character.toUpperCase());
}

function showError(message) {
  statusEl.textContent = "Error";
  const error = document.createElement("p");
  error.className = "error";
  error.textContent = message;
  surfaceEl.prepend(error);
}
