import {bundle} from '@remotion/bundler';
import {renderMedia, selectComposition} from '@remotion/renderer';
import {mkdir, writeFile} from 'node:fs/promises';
import {dirname, join} from 'node:path';

const [sourceFile, outputFile] = process.argv.slice(2);

if (!sourceFile || !outputFile) {
  console.error('Usage: node render.mjs <sourceFile> <outputFile>');
  process.exit(1);
}

const entryPoint = join(dirname(sourceFile), 'index.tsx');
await mkdir(dirname(outputFile), {recursive: true});
await writeFile(entryPoint, `
import React from 'react';
import {Composition, registerRoot} from 'remotion';
import {RemotionKnowledgeVideo} from './KnowledgeVideo';

const Root = () => (
  <Composition
    id="KnowledgeVideo"
    component={RemotionKnowledgeVideo}
    durationInFrames={360}
    fps={30}
    width={1280}
    height={720}
  />
);

registerRoot(Root);
`, 'utf8');

const serveUrl = await bundle({
  entryPoint,
  onProgress: () => undefined,
});

const composition = await selectComposition({
  serveUrl,
  id: 'KnowledgeVideo',
});

await renderMedia({
  composition,
  serveUrl,
  codec: 'h264',
  outputLocation: outputFile,
  chromiumOptions: process.env.CHROME_PATH
    ? {executablePath: process.env.CHROME_PATH}
    : undefined,
});

console.log(`Rendered ${outputFile}`);
