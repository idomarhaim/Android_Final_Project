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
 */
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { delimiter, join } from 'node:path'

const EMULATOR_CMD =
  'firebase emulators:exec --only firestore,functions ' +
  '--project demo-goalpilot --config ../firebase.json ' +
  '"node --test test/triggers.emulator.mjs"'

const env = { ...process.env }
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

const child = spawn(EMULATOR_CMD, { stdio: 'inherit', env, shell: true })
child.on('exit', (code, signal) => process.exit(code ?? (signal ? 1 : 0)))
child.on('error', (err) => {
  console.error(`[run-emulator-tests] could not start firebase-tools: ${err.message}`)
  process.exit(1)
})
