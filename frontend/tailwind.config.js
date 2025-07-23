export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  safelist: [
    'bg-primary',
    'text-primary',
    'hover:bg-green-700',
    'rounded',
    'bg-gray-300',
    'hover:bg-gray-400',
    'text-gray-800',
  ],
  theme: {
    extend: {
      colors: {
        primary: '#4caf50',
        danger: '#dc2626',
        info: '#0284c7',
      },
    },
  },
  plugins: [],
}