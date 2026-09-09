import { mkdir, writeFile } from 'node:fs/promises'

const siteUrl = 'https://dianmingfront.hwaxy.cn'
const outputDir = new URL('../dist/build/h5/', import.meta.url)
const updatedAt = '2026-09-08'

const pages = [
  {
    slug: 'teacher',
    eyebrow: 'FOR TEACHERS · 老师端',
    title: '把课堂秩序，变成每个人的参与感。',
    description:
      '点名星球老师端支持学生名单导入、活动创建、随机点名、课堂加分、共享老师和轮次管理。',
    lead:
      '老师通过一个活动组织一组学生，可以选择重复或不重复点名。随机结果由服务端生成，多位共享老师看到同一个轮次和待处理结果。',
    facts: [
      ['名单管理', '通过 Excel 批量导入学生，也可以在后台逐个维护账号。'],
      ['随机点名', '支持重复抽取和当前轮次不重复抽取，全部抽完后由老师手动开启新轮次。'],
      ['课堂激励', '每次结果可选择加 1 分或不加分，操作只会生效一次。'],
      ['协作教学', '创建者可以邀请其他老师共享活动、成员和点名轮次。'],
    ],
    questions: [
      ['老师如何开始使用点名星球？', '先在管理后台创建学生账号或导入名单，再创建活动、选择成员和点名模式。登录小程序或 H5 后即可进入活动开始点名。'],
      ['共享老师可以做什么？', '共享老师拥有活动编辑、成员管理、点名、加分和轮次重置权限，但不能修改其他老师名下的学生账号。'],
    ],
  },
  {
    slug: 'student',
    eyebrow: 'FOR STUDENTS · 学生端',
    title: '每一次举手，都能看见成长。',
    description:
      '点名星球学生端为每个学生和活动独立保存积分、等级、宠物成长进度及个人加分记录。',
    lead:
      '学生使用学号或手机号登录，无需选择身份。系统根据账号角色进入学生页面，每个活动中的积分与宠物成长互不影响。',
    facts: [
      ['自动识别身份', '登录界面不提供角色选择，服务端根据真实账号角色决定可访问的页面。'],
      ['专属成长伙伴', '首次进入可从猫、兔、龙中选择一只宠物，选定后陪伴整个活动。'],
      ['清晰成长规则', '初始等级为 1，每获得 10 分提升 1 级，页面显示当前进度。'],
      ['个人数据隔离', '学生只能查看自己的积分、宠物和加分记录，不能查看或修改其他学生数据。'],
    ],
    questions: [
      ['积分和等级如何计算？', '每次老师确认加分后增加 1 分。等级等于 1 加上积分除以 10 的向下取整，因此从 9 分增加到 10 分时会从 1 级升到 2 级。'],
      ['被移出活动后数据会消失吗？', '不会。学生暂时失去该活动的访问权，之后重新加入同一个活动时，原有积分和宠物成长会恢复。'],
    ],
  },
  {
    slug: 'random-roll-call',
    eyebrow: 'RANDOM ROLL CALL · 点名规则',
    title: '随机不是动画，是一套可追溯的课堂规则。',
    description:
      '了解点名星球随机点名的重复模式、不重复模式、待确认结果、轮次重置和并发防重复机制。',
    lead:
      '点名结果由服务端从有效活动成员中随机产生，前端动画只负责展示结果。每个活动同一时间只能有一个待确认结果，处理完成后才能继续抽取。',
    facts: [
      ['重复模式', '每次都从全部有效成员中抽取，同一学生以后仍可能再次被抽中。'],
      ['不重复模式', '从本轮尚未抽中的有效成员中抽取，直到候选成员全部抽完。'],
      ['结果确认', '老师必须选择加 1 分或不加分，结果只能处理一次。'],
      ['轮次安全', '存在待确认结果时，不能重置轮次、切换模式或移除被抽中的学生。'],
    ],
    questions: [
      ['点名结果在哪里生成？', '结果由 Java 服务端生成，而不是由浏览器或小程序动画决定。这样共享老师可以看到相同结果，也能避免客户端篡改。'],
      ['新增学生会参加当前轮次吗？', '会。新成员进入当前轮次候选池；已经抽中过的学生移除后重新加入，不会恢复本轮抽取资格。'],
      ['重置轮次会清空积分吗？', '不会。轮次与积分相互独立，重置只恢复点名候选状态。'],
    ],
  },
  {
    slug: 'multi-teacher',
    eyebrow: 'TEAM TEACHING · 多老师协作',
    title: '同一个课堂，多位老师保持同一步调。',
    description:
      '点名星球支持活动创建者邀请共享老师，共同管理成员、随机点名、课堂加分和轮次状态。',
    lead:
      '活动创建者和共享老师共同使用活动，点名轮次、候选成员和待确认结果由服务端统一保存。撤销共享后权限立即失效，已经加入活动的学生仍然保留。',
    facts: [
      ['统一轮次', '不同老师登录后看到相同的抽取进度和待确认学生。'],
      ['完整活动权限', '共享老师可以编辑活动、管理成员、继续共享、点名、加分和重置轮次。'],
      ['账号边界', '活动协作不会开放其他老师的账号管理权限。'],
      ['即时撤权', '撤销共享后老师立即失去活动权限，历史业务数据继续保留。'],
    ],
    questions: [
      ['共享老师的学生能加入活动吗？', '可以。成员选择范围是活动创建者和当前共享老师名下学生的合集。'],
      ['活动创建者能转移吗？', '不能。创建者身份固定，不支持移除或转移。'],
    ],
  },
  {
    slug: 'faq',
    eyebrow: 'QUESTIONS & ANSWERS · 常见问题',
    title: '关于课堂点名，你可能想先问这些。',
    description:
      '点名星球常见问题：账号角色、名单导入、随机点名、积分等级、活动共享、数据权限和部署方式。',
    lead:
      '这里集中说明点名星球的身份识别、点名规则、积分成长与数据权限。所有答案均对应当前版本的真实功能。',
    facts: [
      ['无需选择角色', '系统根据登录账号的真实角色自动进入老师端或学生端。'],
      ['不公开注册', '学生账号由老师或管理员创建，支持学号或手机号登录。'],
      ['活动之间隔离', '同一个学生在不同活动中的积分和宠物成长分别保存。'],
      ['真实接口鉴权', '页面显示不是权限依据，服务端会校验每一次资源访问。'],
    ],
    questions: [
      ['学生名单可以批量导入吗？', '可以。后台提供 Excel 模板，先整批校验并展示错误行，确认无误后再一次性写入，不会产生部分导入。'],
      ['学生可以自己修改积分吗？', '不可以。积分只能由具备活动权限的老师处理点名结果时产生。'],
      ['管理员能进入小程序业务端吗？', '不能。系统管理员只使用管理后台；学生不能登录管理后台。'],
      ['Redis 是必须的吗？', '是。Redis 保存登录会话和登录限流信息，Redis 不可用时服务端不会绕过认证。'],
      ['支持哪些终端？', '学生和老师端支持微信小程序与 H5，管理员和老师可以使用 Vue 管理后台维护完整业务数据。'],
    ],
  },
  {
    slug: 'about',
    eyebrow: 'ABOUT DIANMING PLANET · 关于我们',
    title: '让每一次参与，都被温柔看见。',
    description:
      '点名星球专注课堂随机点名与学生成长激励，让老师更轻松地组织互动，让学生看见自己的每一点进步。',
    lead:
      '点名星球从一个很小的课堂瞬间出发：当名字被点到，学生获得的不只是一次回答机会，也可以是一段持续可见的成长。',
    facts: [
      ['我们的目标', '降低老师组织课堂互动的成本，让随机点名更加公平、清楚和可追溯。'],
      ['成长而非排名', '积分只用于记录个人参与和宠物成长，不鼓励公开比较学生。'],
      ['清晰的边界', '老师管理课堂，学生查看本人数据，系统管理员维护账号与全局设置。'],
      ['持续改进', '产品围绕真实课堂流程设计，功能说明和规则会随版本及时更新。'],
    ],
    questions: [
      ['点名星球适合谁？', '适合希望通过随机提问、课堂加分和轻量游戏化成长提升学生参与感的老师、班级和培训课堂。'],
      ['它与普通随机抽签有什么不同？', '除了生成随机结果，点名星球还维护活动成员、共享轮次、结果确认、积分流水和每位学生的宠物成长。'],
    ],
  },
]

const css = `
:root{--ink:#21352c;--paper:#f5f2e8;--white:#fffdf7;--green:#176b4d;--leaf:#9fbc58;--orange:#df7041;--line:rgba(33,53,44,.18)}
*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--paper);color:var(--ink);font-family:"PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif;line-height:1.75}
body:before{content:"";position:fixed;inset:0;pointer-events:none;opacity:.22;background-image:radial-gradient(rgba(33,53,44,.18) .6px,transparent .6px);background-size:7px 7px}
a{color:inherit}.wrap{width:min(1120px,calc(100% - 40px));margin:auto}.topbar{display:flex;align-items:center;justify-content:space-between;padding:24px 0;border-bottom:1px solid var(--line)}
.brand{display:flex;align-items:center;gap:11px;font-family:"Songti SC","Noto Serif SC",serif;font-weight:900;font-size:20px;text-decoration:none}.stamp{display:grid;place-items:center;width:36px;height:36px;background:var(--orange);color:white;border-radius:50% 47% 52% 45%;transform:rotate(-5deg)}
.nav{display:flex;gap:22px;font-size:14px}.nav a{text-decoration:none}.nav a:hover{text-decoration:underline;text-underline-offset:5px}.login{padding:9px 17px!important;border:1px solid var(--ink);border-radius:100px}
.hero{display:grid;grid-template-columns:minmax(0,1.25fr) minmax(280px,.75fr);gap:72px;align-items:center;padding:96px 0 72px}.eyebrow{color:var(--orange);font-size:12px;font-weight:800;letter-spacing:.18em}.hero h1{margin:20px 0 28px;font-family:"Songti SC","Noto Serif SC",serif;font-size:clamp(44px,7.1vw,86px);line-height:1.08;letter-spacing:-.055em}.lead{max-width:780px;font-size:19px;line-height:1.9;color:#486057}
.planet{position:relative;aspect-ratio:1;display:grid;place-items:center}.orbit{position:absolute;inset:6%;border:1px solid var(--line);border-radius:50%;animation:spin 24s linear infinite}.orbit:after{content:"";position:absolute;width:18px;height:18px;background:var(--leaf);border-radius:50%;top:11%;left:11%}.planet img{width:64%;filter:drop-shadow(0 22px 24px rgba(33,53,44,.13));animation:float 4.5s ease-in-out infinite}
.number{position:absolute;right:2%;bottom:4%;font-family:"Songti SC",serif;font-size:64px;color:rgba(23,107,77,.16)}.answer{background:var(--ink);color:var(--white);padding:58px;border-radius:4px 42px 4px 4px;position:relative;overflow:hidden}.answer:after{content:"点";position:absolute;right:28px;bottom:-58px;font:200px/1 "Songti SC",serif;color:rgba(255,255,255,.045)}
.answer h2,.section h2{font-family:"Songti SC","Noto Serif SC",serif;font-size:clamp(28px,4vw,46px);line-height:1.25;margin:0 0 20px}.answer p{max-width:820px;margin:0;font-size:18px;line-height:2;color:#e6eadf}
.section{padding:86px 0}.section-head{display:flex;align-items:end;justify-content:space-between;gap:30px;margin-bottom:38px}.section-head p{max-width:480px;color:#62756d}.facts{display:grid;grid-template-columns:repeat(2,1fr);border-top:1px solid var(--line);border-left:1px solid var(--line)}.fact{min-height:220px;padding:34px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);background:rgba(255,253,247,.46)}.fact-index{font:700 11px/1 monospace;color:var(--orange)}.fact h3{font-family:"Songti SC",serif;font-size:25px;margin:38px 0 12px}.fact p{margin:0;color:#5c6e66}
.qa{display:grid;grid-template-columns:1fr 1fr;gap:18px}.qa article{background:var(--white);padding:30px;border-radius:4px 24px 4px 4px;border:1px solid rgba(33,53,44,.08)}.qa h3{font:700 19px/1.5 "Songti SC",serif;margin:0 0 13px}.qa p{margin:0;color:#596c63}
.cta{margin:22px auto 82px;padding:54px;background:var(--orange);color:white;display:flex;align-items:center;justify-content:space-between;gap:30px;border-radius:4px 42px 4px 4px}.cta h2{font:700 clamp(27px,4vw,44px)/1.3 "Songti SC",serif;margin:0}.cta a{display:inline-block;background:white;color:var(--ink);padding:13px 22px;border-radius:100px;text-decoration:none;white-space:nowrap;font-weight:700}
footer{border-top:1px solid var(--line);padding:30px 0 50px;color:#687a72;font-size:13px}.footer-inner{display:flex;justify-content:space-between;gap:20px}.footer-links{display:flex;gap:18px}
@keyframes spin{to{transform:rotate(360deg)}}@keyframes float{50%{transform:translateY(-12px) rotate(2deg)}}@media(max-width:760px){.nav a:not(.login){display:none}.hero{grid-template-columns:1fr;gap:26px;padding:62px 0 46px}.hero h1{font-size:48px}.planet{width:260px;margin:auto}.answer{padding:36px 25px}.section{padding:60px 0}.section-head{display:block}.facts,.qa{grid-template-columns:1fr}.fact{min-height:auto}.cta{margin-bottom:54px;padding:35px 25px;display:block}.cta a{margin-top:22px}.footer-inner{display:block}.footer-links{margin-top:12px;flex-wrap:wrap}}
@media(prefers-reduced-motion:reduce){*{animation:none!important;scroll-behavior:auto!important}}
`

function escapeJson(value) {
  return JSON.stringify(value).replace(/</g, '\\u003c')
}

function renderPage(page, pageNumber) {
  const canonical = `${siteUrl}/${page.slug}/`
  const facts = page.facts
    .map(
      (fact, index) =>
        `<article class="fact"><span class="fact-index">0${index + 1}</span><h3>${fact[0]}</h3><p>${fact[1]}</p></article>`,
    )
    .join('')
  const questions = page.questions
    .map((item) => `<article><h3>${item[0]}</h3><p>${item[1]}</p></article>`)
    .join('')
  const jsonLd = {
    '@context': 'https://schema.org',
    '@graph': [
      {
        '@type': 'WebPage',
        name: page.title,
        description: page.description,
        url: canonical,
        dateModified: updatedAt,
        inLanguage: 'zh-CN',
        isPartOf: { '@type': 'WebSite', name: '点名星球', url: siteUrl },
      },
      {
        '@type': 'BreadcrumbList',
        itemListElement: [
          { '@type': 'ListItem', position: 1, name: '点名星球', item: `${siteUrl}/` },
          { '@type': 'ListItem', position: 2, name: page.eyebrow.split('·').at(-1).trim(), item: canonical },
        ],
      },
      {
        '@type': 'FAQPage',
        mainEntity: page.questions.map((item) => ({
          '@type': 'Question',
          name: item[0],
          acceptedAnswer: { '@type': 'Answer', text: item[1] },
        })),
      },
    ],
  }

  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>${page.title}｜点名星球</title>
  <meta name="description" content="${page.description}">
  <meta name="robots" content="index,follow,max-image-preview:large,max-snippet:-1">
  <link rel="canonical" href="${canonical}">
  <link rel="icon" href="/static/dianming-planet-avatar.png">
  <meta property="og:type" content="article"><meta property="og:locale" content="zh_CN">
  <meta property="og:site_name" content="点名星球"><meta property="og:title" content="${page.title}">
  <meta property="og:description" content="${page.description}"><meta property="og:url" content="${canonical}">
  <meta property="og:image" content="${siteUrl}/static/dianming-planet-avatar.png">
  <script type="application/ld+json">${escapeJson(jsonLd)}</script>
  <style>${css}</style>
</head>
<body>
  <header class="wrap topbar">
    <a class="brand" href="/"><span class="stamp">点</span><span>点名星球</span></a>
    <nav class="nav" aria-label="主要导航"><a href="/teacher/">老师端</a><a href="/student/">学生端</a><a href="/faq/">常见问题</a><a class="login" href="/#/pages/login/login">进入应用 ↗</a></nav>
  </header>
  <main>
    <section class="wrap hero">
      <div><div class="eyebrow">${page.eyebrow}</div><h1>${page.title}</h1><p class="lead">${page.description}</p></div>
      <div class="planet" aria-hidden="true"><div class="orbit"></div><img src="/static/dianming-planet-avatar.png" alt=""><span class="number">0${pageNumber}</span></div>
    </section>
    <section class="wrap answer" aria-labelledby="direct-answer"><h2 id="direct-answer">先说答案</h2><p>${page.lead}</p></section>
    <section class="wrap section"><div class="section-head"><h2>功能与规则</h2><p>每条说明均对应当前版本的实际行为，便于老师、学生和搜索系统准确理解产品。</p></div><div class="facts">${facts}</div></section>
    <section class="wrap section"><div class="section-head"><h2>常见问题</h2><p>围绕真实使用场景，给出可以直接验证的答案。</p></div><div class="qa">${questions}</div></section>
    <aside class="wrap cta"><h2>点到名字，<br>也点亮成长。</h2><a href="/#/pages/login/login">登录点名星球</a></aside>
  </main>
  <footer><div class="wrap footer-inner"><span>© 2026 点名星球 · 更新于 ${updatedAt}</span><div class="footer-links"><a href="/">产品首页</a><a href="/random-roll-call/">点名规则</a><a href="/multi-teacher/">老师协作</a><a href="/about/">关于我们</a></div></div></footer>
</body>
</html>`
}

await mkdir(outputDir, { recursive: true })
await Promise.all(
  pages.map(async (page, index) => {
    const directory = new URL(`./${page.slug}/`, outputDir)
    await mkdir(directory, { recursive: true })
    await writeFile(new URL('index.html', directory), renderPage(page, index + 1), 'utf8')
  }),
)

const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url><loc>${siteUrl}/</loc><lastmod>${updatedAt}</lastmod></url>
${pages.map((page) => `  <url><loc>${siteUrl}/${page.slug}/</loc><lastmod>${updatedAt}</lastmod></url>`).join('\n')}
</urlset>
`

const robots = `User-agent: *
Allow: /

User-agent: OAI-SearchBot
Allow: /

Sitemap: ${siteUrl}/sitemap.xml
`

const aiSummary = `# 点名星球

点名星球是一款面向老师和学生的课堂随机点名与成长激励小程序。
老师可以导入学生名单、创建活动、共享其他老师、随机点名并处理课堂加分。
学生可以查看本人积分、等级、宠物成长和加分记录。

核心功能：
- 重复点名与当前轮次不重复点名
- Excel 学生名单整批校验和导入
- 多老师共享活动与点名轮次
- 每次结果加 1 分或不加分，且只能处理一次
- 学生与活动维度独立保存积分和宠物
- 服务端身份、资源权限和并发一致性校验

产品首页：${siteUrl}/
老师端说明：${siteUrl}/teacher/
学生端说明：${siteUrl}/student/
随机点名规则：${siteUrl}/random-roll-call/
多老师协作：${siteUrl}/multi-teacher/
常见问题：${siteUrl}/faq/
关于我们：${siteUrl}/about/

本文件是面向文本读取工具的辅助摘要，页面正文是产品事实的最终说明。
`

await writeFile(new URL('sitemap.xml', outputDir), sitemap, 'utf8')
await writeFile(new URL('robots.txt', outputDir), robots, 'utf8')
await writeFile(new URL('llms.txt', outputDir), aiSummary, 'utf8')

console.log(`Generated ${pages.length} GEO pages, sitemap.xml, robots.txt and llms.txt`)
