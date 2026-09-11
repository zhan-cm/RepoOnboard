export const CURRENT_REPORT_SCHEMA = '1.2'

export function reportSchemaCompatibility(value) {
  if (typeof value !== 'string' || !/^(0|[1-9]\d*)\.(0|[1-9]\d*)$/.test(value)) {
    return { compatible: false, reason: 'invalid' }
  }
  const [actualMajor] = value.split('.').map(Number)
  const [supportedMajor] = CURRENT_REPORT_SCHEMA.split('.').map(Number)
  return {
    compatible: actualMajor === supportedMajor,
    reason: actualMajor === supportedMajor ? 'compatible' : 'unsupported-major'
  }
}

export function verifyPackagedSchema(documentRoot = document) {
  const packagedVersion = documentRoot
    .querySelector('meta[name="repoonboard-report-schema"]')
    ?.getAttribute('content')
  if (packagedVersion !== CURRENT_REPORT_SCHEMA) {
    throw new Error(`Packaged UI schema mismatch (expected ${CURRENT_REPORT_SCHEMA})`)
  }
}
