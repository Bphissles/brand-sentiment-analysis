/**
 * Composable for managing dark/light mode
 * Uses a watcher to reactively update the DOM when colorMode changes
 */
export const useColorMode = () => {
  const colorMode = useState<'light' | 'dark'>('colorMode', () => 'light')

  const isDark = computed(() => colorMode.value === 'dark')

  // Watch for changes and update DOM reactively
  watch(colorMode, (newMode) => {
    if (process.client) {
      if (newMode === 'dark') {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
      localStorage.setItem('colorMode', newMode)
    }
  }, { immediate: false })

  const toggle = () => {
    colorMode.value = colorMode.value === 'light' ? 'dark' : 'light'
  }

  const setMode = (mode: 'light' | 'dark') => {
    colorMode.value = mode
  }

  const loadPreference = () => {
    if (process.client) {
      const saved = localStorage.getItem('colorMode') as 'light' | 'dark' | null
      if (saved) {
        colorMode.value = saved
      } else {
        // Check system preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
        colorMode.value = prefersDark ? 'dark' : 'light'
      }
      // Apply immediately on load
      if (colorMode.value === 'dark') {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    }
  }

  return {
    colorMode,
    isDark,
    toggle,
    setMode,
    loadPreference
  }
}
