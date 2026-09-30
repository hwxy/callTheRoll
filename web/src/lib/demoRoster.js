import { strFromU8, strToU8, unzipSync, zipSync } from 'fflate'

const templateRows = ['姓名', '林小满', '陈星野', '许知夏']

export function createXlsxRosterTemplate() {
  const rows = templateRows.map((name, index) =>
    `<row r="${index + 1}"><c r="A${index + 1}" t="inlineStr"><is><t>${name}</t></is></c></row>`,
  ).join('')
  const files = {
    '[Content_Types].xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>`,
    '_rels/.rels': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>`,
    'xl/workbook.xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="名单" sheetId="1" r:id="rId1"/></sheets></workbook>`,
    'xl/_rels/workbook.xml.rels': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>`,
    'xl/worksheets/sheet1.xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:A${templateRows.length}"/><sheetData>${rows}</sheetData></worksheet>`,
  }
  return zipSync(Object.fromEntries(Object.entries(files).map(([path, xml]) => [path, strToU8(xml)])))
}

function decodeXml(value = '') {
  return value
    .replace(/&#x([\da-f]+);/gi, (_, code) => String.fromCodePoint(parseInt(code, 16)))
    .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&')
}

function readTag(xml, tag) {
  return [...xml.matchAll(new RegExp(`<${tag}\\b[^>]*>([\\s\\S]*?)<\\/${tag}>`, 'g'))].map(
    (match) => decodeXml(match[1].replace(/<[^>]+>/g, '')),
  )
}

function columnIndex(reference = '') {
  const letters = reference.match(/^[A-Z]+/i)?.[0]?.toUpperCase() || 'A'
  return [...letters].reduce((index, letter) => index * 26 + letter.charCodeAt(0) - 64, 0) - 1
}

function readSharedStrings(xml) {
  return [...xml.matchAll(/<si\b[^>]*>([\s\S]*?)<\/si>/g)].map((match) =>
    readTag(match[1], 't').join(''),
  )
}

function readRows(xml, sharedStrings) {
  return [...xml.matchAll(/<row\b[^>]*>([\s\S]*?)<\/row>/g)].map((row) => {
    const cells = []
    for (const cell of row[1].matchAll(/<c\b([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attributes = cell[1]
      const body = cell[2] || ''
      const reference = attributes.match(/\br="([^"]+)"/)?.[1] || ''
      const type = attributes.match(/\bt="([^"]+)"/)?.[1] || ''
      let value = ''

      if (type === 'inlineStr') value = readTag(body, 't').join('')
      else {
        const raw = readTag(body, 'v')[0] || ''
        value = type === 's' ? sharedStrings[Number(raw)] || '' : raw
      }

      cells[columnIndex(reference)] = value.trim()
    }
    return cells
  })
}

function isNameHeader(value = '') {
  const normalized = value.toLowerCase().replace(/[\s_*（）()【】\[\]：:]/g, '')
  return ['姓名', '学生姓名', '名字', 'name', 'studentname', 'student'].includes(normalized)
}

function resolveSheetPath(workbookXml, relationshipsXml) {
  const relationshipId = workbookXml.match(/<sheet\b[^>]*\br:id="([^"]+)"/)?.[1]
  if (!relationshipId) throw new Error('Excel 文件中没有可读取的工作表')

  const relation = [...relationshipsXml.matchAll(/<Relationship\b([^>]*?)\/?\s*>/g)].find(
    (match) => match[1].match(/\bId="([^"]+)"/)?.[1] === relationshipId,
  )
  const target = relation?.[1].match(/\bTarget="([^"]+)"/)?.[1]
  if (!target) throw new Error('无法读取 Excel 的第一个工作表')
  return target.startsWith('/') ? target.slice(1) : `xl/${target.replace(/^\.\//, '')}`
}

export function parseXlsxRoster(fileBytes) {
  const archive = unzipSync(fileBytes)
  const readFile = (path) => (archive[path] ? strFromU8(archive[path]) : '')
  const workbookXml = readFile('xl/workbook.xml')
  const relationshipsXml = readFile('xl/_rels/workbook.xml.rels')

  if (!workbookXml || !relationshipsXml) throw new Error('无法读取 Excel 文件，请确认文件格式为 .xlsx')

  const sheetPath = resolveSheetPath(workbookXml, relationshipsXml)
  const sheetXml = readFile(sheetPath)
  if (!sheetXml) throw new Error('Excel 的第一个工作表为空或格式不受支持')

  const sharedStrings = readSharedStrings(readFile('xl/sharedStrings.xml'))
  const rows = readRows(sheetXml, sharedStrings).filter((row) => row.some(Boolean))
  if (!rows.length) throw new Error('Excel 中没有找到姓名')

  const headerIndex = rows[0].findIndex(isNameHeader)
  const nameColumn = headerIndex >= 0 ? headerIndex : 0
  const names = rows
    .slice(headerIndex >= 0 ? 1 : 0)
    .map((row) => row[nameColumn] || '')
    .map((name) => name.trim())
    .filter(Boolean)

  if (!names.length) throw new Error('没有找到姓名，请将姓名放在第一列或添加“姓名”表头')
  return names
}
