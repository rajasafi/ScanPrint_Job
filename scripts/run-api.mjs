import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const backend = path.join(root, 'backend');
const wrapperJar = path.join(backend, '.mvn', 'wrapper', 'maven-wrapper.jar');
const javaArgs = [
  `-Dmaven.multiModuleProjectDirectory=${backend}`,
  '-classpath', wrapperJar,
  'org.apache.maven.wrapper.MavenWrapperMain',
  'spring-boot:run',
];
const env = {
  ...process.env,
  SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'dev',
  // A fresh, process-local signing key is safe for throwaway local development.
  // Production and packaged-artifact runs must provide JWT_SECRET themselves.
  JWT_SECRET: process.env.JWT_SECRET || randomBytes(48).toString('base64'),
  PRINTDOC_SEED_ENABLED: process.env.PRINTDOC_SEED_ENABLED || 'true',
  BOOTSTRAP_ADMIN_ENABLED: process.env.BOOTSTRAP_ADMIN_ENABLED || 'false',
};
console.info('Starting PrintDoc API on localhost:8080 (local dev profile).');
console.info('Ephemeral JWT secret is process-local; sessions will expire after restart.');
const child = spawn('java', javaArgs, { cwd: backend, env, stdio: 'inherit', shell: false });
child.on('error', (error) => { console.error('Could not start Java/Maven wrapper:', error.message); process.exitCode = 1; });
child.on('exit', (code, signal) => {
  if (signal) { console.info(`API stopped (${signal}).`); process.exitCode = 1; }
  else process.exitCode = code ?? 1;
});
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => child.kill(signal));
