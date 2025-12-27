import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useColorMode } from '../../composables/useColorMode'

describe('useColorMode', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  describe('initial state', () => {
    it('should start with light mode', () => {
      const { colorMode, isDark } = useColorMode()
      expect(colorMode.value).toBe('light')
      expect(isDark.value).toBe(false)
    })
  })

  describe('toggle', () => {
    it('should toggle from light to dark', () => {
      const { colorMode, toggle, isDark } = useColorMode()
      expect(colorMode.value).toBe('light')

      toggle()

      expect(colorMode.value).toBe('dark')
      expect(isDark.value).toBe(true)
    })

    it('should toggle from dark to light', () => {
      const { colorMode, toggle, setMode } = useColorMode()
      setMode('dark')

      toggle()

      expect(colorMode.value).toBe('light')
    })
  })

  describe('setMode', () => {
    it('should set mode to dark', () => {
      const { colorMode, setMode, isDark } = useColorMode()

      setMode('dark')

      expect(colorMode.value).toBe('dark')
      expect(isDark.value).toBe(true)
    })

    it('should set mode to light', () => {
      const { colorMode, setMode, isDark } = useColorMode()
      setMode('dark')

      setMode('light')

      expect(colorMode.value).toBe('light')
      expect(isDark.value).toBe(false)
    })
  })

  describe('loadPreference', () => {
    it('should load saved preference from localStorage', () => {
      localStorage.setItem('colorMode', 'dark')

      const { colorMode, loadPreference } = useColorMode()
      loadPreference()

      expect(colorMode.value).toBe('dark')
    })

    it('should apply dark class to document when loading dark preference', () => {
      localStorage.setItem('colorMode', 'dark')

      const { loadPreference } = useColorMode()
      loadPreference()

      expect(document.documentElement.classList.contains('dark')).toBe(true)
    })

    it('should remove dark class when loading light preference', () => {
      document.documentElement.classList.add('dark')
      localStorage.setItem('colorMode', 'light')

      const { loadPreference } = useColorMode()
      loadPreference()

      expect(document.documentElement.classList.contains('dark')).toBe(false)
    })
  })

  describe('isDark computed', () => {
    it('should return true when mode is dark', () => {
      const { setMode, isDark } = useColorMode()
      setMode('dark')
      expect(isDark.value).toBe(true)
    })

    it('should return false when mode is light', () => {
      const { setMode, isDark } = useColorMode()
      setMode('light')
      expect(isDark.value).toBe(false)
    })
  })
})
