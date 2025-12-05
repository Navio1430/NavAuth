import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "NavAuth Docs",
  description: "project dcumentation",
  themeConfig: {
    logo: '/navauth-logo.svg',
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Docs', link: '/markdown-examples' }
    ],

    sidebar: [
      {
        text: 'Examples',
        items: [
          { text: 'View documentation', link: '/markdown-examples' },
          { text: 'Runtime API Examples', link: '/api-examples' }
        ]
      }
    ],

    footer: {
      message: "Made by Navio1430 and Contributors with ❤️",
      copyright: `Copyright © ${new Date().getFullYear()} Sapphire Code sp. z o.o. & Oliwier Fijas
        <br>Powered by Sapphire Code <img src='/sapphire-logo.svg' width='16' alt='Sapphire Code' style='vertical-align: middle; display: inline;'>`,
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Navio1430' },
      { icon: 'discord', link: 'todo' }
    ]
  }
})
