import { readdir, rm } from 'node:fs/promises'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const staticDirectory = fileURLToPath(new URL('../dist/build/mp-weixin/static/', import.meta.url))
const imagePattern = /\.(?:png|jpe?g|webp|gif|svg)$/i

async function removeImages(directory) {
  let entries
  try {
    entries = await readdir(directory, { withFileTypes: true })
  } catch (error) {
    if (error.code === 'ENOENT') return
    throw error
  }

  for (const entry of entries) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) {
      await removeImages(path)
      const remaining = await readdir(path)
      if (!remaining.length) await rm(path, { recursive: true })
    } else if (entry.isFile() && imagePattern.test(entry.name)) {
      await rm(path)
    }
  }
}

await removeImages(staticDirectory)
console.log('Removed bundled mini-program images; the help screenshot loads from the H5 HTTPS site.')
