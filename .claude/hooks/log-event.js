#!/usr/bin/env node
// Claude Code hook: appends a JSONL record of prompts/tool calls/session-end
// markers to logs/claude-activity.jsonl for later review or offline analysis.
const fs = require('fs');
const path = require('path');

const eventName = process.argv[2] || 'Unknown';

let raw = '';
try {
  raw = fs.readFileSync(0, 'utf-8');
} catch (e) {
  raw = '';
}

let input = {};
try {
  input = raw ? JSON.parse(raw) : {};
} catch (e) {
  input = { parse_error: String(e && e.message || e) };
}

const PREVIEW_LIMIT = 2000;
function previewValue(value) {
  const json = JSON.stringify(value);
  if (json === undefined) return undefined;
  if (json.length <= PREVIEW_LIMIT) return value;
  return { truncated: true, preview: json.slice(0, PREVIEW_LIMIT) + '...' };
}

const logDir = path.join(__dirname, '..', '..', 'logs');
const logFile = path.join(logDir, 'claude-activity.jsonl');

const entry = {
  timestamp: new Date().toISOString(),
  event: eventName,
  session_id: input.session_id,
};

if (eventName === 'UserPromptSubmit') {
  entry.prompt = input.prompt;
} else if (eventName === 'PostToolUse') {
  entry.tool_name = input.tool_name;
  entry.tool_input = previewValue(input.tool_input);
  entry.success = input.tool_response ? input.tool_response.success : undefined;
} else if (eventName === 'Stop') {
  entry.transcript_path = input.transcript_path;
} else {
  entry.data = previewValue(input);
}

try {
  fs.mkdirSync(logDir, { recursive: true });
  fs.appendFileSync(logFile, JSON.stringify(entry) + '\n');
} catch (e) {
  // Never block Claude Code on a logging failure.
}

process.exit(0);
