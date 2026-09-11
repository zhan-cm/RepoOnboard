import { describe, expect, it } from 'vitest'
import {
  CURRENT_REPORT_SCHEMA,
  reportSchemaCompatibility,
  verifyPackagedSchema
} from '../lib/reportSchema.js'

describe('report schema boundary', () => {
  it('accepts canonical versions in the supported major line', () => {
    expect(CURRENT_REPORT_SCHEMA).toBe('1.2')
    expect(reportSchemaCompatibility('1.0').compatible).toBe(true)
    expect(reportSchemaCompatibility('1.9').compatible).toBe(true)
  })

  it('rejects invalid and unsupported major versions', () => {
    expect(reportSchemaCompatibility('2.0')).toEqual({
      compatible: false,
      reason: 'unsupported-major'
    })
    expect(reportSchemaCompatibility('v1')).toEqual({ compatible: false, reason: 'invalid' })
    expect(reportSchemaCompatibility(null)).toEqual({ compatible: false, reason: 'invalid' })
  })

  it('requires the packaged document marker to match the application build', () => {
    const documentRoot = document.implementation.createHTMLDocument('test')
    const marker = documentRoot.createElement('meta')
    marker.name = 'repoonboard-report-schema'
    marker.content = CURRENT_REPORT_SCHEMA
    documentRoot.head.append(marker)

    expect(() => verifyPackagedSchema(documentRoot)).not.toThrow()
    marker.content = '2.0'
    expect(() => verifyPackagedSchema(documentRoot)).toThrow('Packaged UI schema mismatch')
  })
})
