#!/usr/bin/env node
/**
 * Runs `triggers.emulator.mjs` against the Firestore **and Functions** emulators.
 *
 * This is `firestore-tests/run-tests.mjs`'s sibling and exists for the same reason: `firebase
 * emulators:exec` needs Java >= 21 and finds it by looking up `java` on `PATH`, **not** through
 * `JAVA_HOME`. The two comments worth not re-discovering are copied from that file because they
 * cost a session each:
 *
 *  - The command is **one string**, not an args array. With `shell: true` an array is flattened by
 *    plain concatenation, which strips the quotes around the script argument and leaves
 *    firebase-tools parsing `--test` as one of its own options.
 *  - Windows environment variable names are case-insensitive and Node hands them back with the
 *    OS's casing — normally `Path`. Writing `env.PATH` **adds a second key**; the spawned shell
 *    reads the untouched original, and worse, the key that wins is not the one npm augmented with
 *    `node_modules/.bin`, so `firebase` itself stops resolving.
 *
 * What is different here: `--only firestore,functions`. The functions emulator loads
 * `lib/index.js`, so `npm run build` must have run first — `package.json` chains it.
 *
 * ### And it needs `FUNCTIONS_DISCOVERY_TIMEOUT`, or EVERY test fails for the wrong reason
 *
 * The analyzer's default budget for reading the backend spec out of `lib/index.js` is **10 s**,
 * and on this machine it is not enough. What you get is one warning line —
 *
 *     !! functions: Failed to load function definition from source: FirebaseError: User code
 *        failed to load. Cannot determine backend specification. Timeout after 10000.
 *
 * — and then the suite runs to completion with **no functions registered at all**, so every
 * trigger assertion times out at 15 s. `Observed:` 2026-08-24, session `challenge-scoring`:
 * **15 of 17 failed**, the two that passed being the two that assert a trigger does *nothing*.
 * That reads exactly like "the new trigger broke the old ones" and is nothing of the kind.
 *
 * **Refute it in one command before touching any code** — the module itself loads fine:
 *
 *     node -e "const t=Date.now();const m=require('./lib/index.js');console.log(Date.now()-t,'ms',Object.keys(m))"
 *
 * `Observed:` **212 ms**, all eight exports listed, immediately after that failure. `CLAUDE.md`
 * records the same trap for `firebase deploy --only functions`, where its message likewise names
 * the wrong cause; it bites `emulators:exec` identically, and this file is where the fix belongs
 * so that nobody has to know that twice.
 */
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { delimiter, join } from 'node:path'

const EMULATOR_CMD =
  'firebase emulators:exec --only firestore,functions ' +
  '--project demo-goalpilot --config ../firebase.json ' +
  '"node --test test/triggers.emulator.mjs"'

const env = { ...process.env }

// See the block comment above. Set rather than overridden, so a caller who already knows they
// need longer keeps their own value.
env.FUNCTIONS_DISCOVERY_TIMEOUT = env.FUNCTIONS_DISCOVERY_TIMEOUT ?? '120'

const javaHome = env.JAVA_HOME
const pathKey = Object.keys(env).find((k) => k.toUpperCase() === 'PATH') ?? 'PATH'

if (!javaHome) {
  console.warn('[run-emulator-tests] JAVA_HOME is not set — using whatever `java` is on PATH.')
} else {
  const bin = join(javaHome, 'bin')
  const exe = join(bin, process.platform === 'win32' ? 'java.exe' : 'java')
  if (existsSync(exe)) {
    env[pathKey] = bin + delimiter + (env[pathKey] ?? '')
    console.log(`[run-emulator-tests] Using the JDK at JAVA_HOME: ${javaHome}`)
  } else {
    console.warn(
      `[run-emulator-tests] JAVA_HOME is set to ${javaHome}, but there is no java there.\n` +
      '[run-emulator-tests] Falling back to PATH; if the emulators refuse to start, that is why.',
    )
  }
}

console.log(
  `[run-emulator-tests] FUNCTIONS_DISCOVERY_TIMEOUT=${env.FUNCTIONS_DISCOVERY_TIMEOUT}s ` +
  '(the 10s default is not enough on this machine — see the header)',
)

const child = spawn(EMULATOR_CMD, { stdio: 'inherit', env, shell: true })
child.on('exit', (code, signal) => process.exit(code ?? (signal ? 1 : 0)))
child.on('error', (err) => {
  console.error(`[run-emulator-tests] could not start firebase-tools: ${err.message}`)
  process.exit(1)
})
