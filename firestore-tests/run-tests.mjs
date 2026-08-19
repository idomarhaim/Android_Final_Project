#!/usr/bin/env node
/**
 * Runs the security-rules suite with a JDK the Firebase emulators will accept.
 *
 * WHY THIS EXISTS. `firebase emulators:exec` needs Java >= 21, and it finds Java
 * by looking up `java` on `PATH` — it does **not** read `JAVA_HOME`. On a machine
 * with several JDKs installed, whichever one happens to sit earliest on `PATH`
 * decides whether this suite runs, and on Windows the system `PATH` is written by
 * the installers rather than by you. Observed 2026-08-15 on this machine: `PATH`
 * offered JDK **17** first, so `npm test` died with
 *
 *   Error: firebase-tools no longer supports Java version before 21.
 *
 * while `JAVA_HOME` pointed at a perfectly good JDK 21 the whole time. Reordering
 * the machine `PATH` needs administrator rights, which a test script has no
 * business requiring — and the repo's own convention (AGENTS.md, CLAUDE.md) is
 * already "build and test from a shell whose JAVA_HOME is JDK 21". So this
 * honours that convention by putting `JAVA_HOME/bin` in front of `PATH` for the
 * emulator's child process only, and changes nothing outside it.
 *
 * It is deliberately not a silent fallback: if JAVA_HOME is missing or does not
 * contain a java binary, this says so and lets the run continue on whatever
 * `PATH` provides, because that is still the correct behaviour on a machine with
 * exactly one modern JDK — and a wrapper that swallowed the difference would hide
 * the next instance of this same problem.
 */
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { delimiter, join } from 'node:path'

// One string, not an args array. With `shell: true` an array is flattened by
// simple concatenation, which strips the quotes around the script argument and
// leaves firebase-tools parsing `--test` as one of its own options
// ("error: unknown option '--test'"). Quoting it here is the whole fix, and it
// also avoids Node's args-plus-shell deprecation warning.
const EMULATOR_CMD =
  'firebase emulators:exec --only firestore,storage ' +
  '--project demo-goalpilot --config ../firebase.json "node --test"'

const env = { ...process.env }
const javaHome = env.JAVA_HOME

// Windows environment variable names are case-insensitive, and Node hands them
// back with the OS's own casing — normally `Path`, not `PATH`. Writing `env.PATH`
// therefore ADDS A SECOND KEY rather than replacing the first, and the spawned
// `cmd.exe` reads the untouched original: the JDK never gets prepended, and
// worse, whichever key wins is not the one npm augmented with `node_modules/.bin`,
// so `firebase` itself stops resolving. Observed here on 2026-08-15 — it fails as
// "'firebase' is not recognized", which points at everything except the real
// cause. Find the existing key and write to that one.
const pathKey = Object.keys(env).find((k) => k.toUpperCase() === 'PATH') ?? 'PATH'

if (!javaHome) {
  console.warn('[run-tests] JAVA_HOME is not set — using whatever `java` is on PATH.')
} else {
  const bin = join(javaHome, 'bin')
  const exe = join(bin, process.platform === 'win32' ? 'java.exe' : 'java')
  if (existsSync(exe)) {
    env[pathKey] = bin + delimiter + (env[pathKey] ?? '')
    console.log(`[run-tests] Using the JDK at JAVA_HOME: ${javaHome}`)
  } else {
    // The exact failure this machine had: JAVA_HOME naming a directory that no
    // longer holds a JDK, left behind by an uninstall.
    console.warn(
      `[run-tests] JAVA_HOME is set to ${javaHome}, but there is no java there.\n` +
      '[run-tests] Falling back to PATH; if the emulators refuse to start, that is why.',
    )
  }
}

// shell: true so Windows resolves `firebase.cmd`. The command is a fixed literal
// defined above and never contains user input.
const child = spawn(EMULATOR_CMD, { stdio: 'inherit', env, shell: true })
child.on('exit', (code, signal) => process.exit(code ?? (signal ? 1 : 0)))
child.on('error', (err) => {
  console.error(`[run-tests] could not start firebase-tools: ${err.message}`)
  process.exit(1)
})
