import { mkdir, writeFile } from 'node:fs/promises'

const siteUrl = 'https://dianmingfront.hwaxy.cn'
const outputDir = new URL('../dist/build/h5/', import.meta.url)
const updatedAt = new Date().toISOString().slice(0, 10)

const pages = [
  {
    slug: 'teacher',
    eyebrow: 'FOR TEACHERS · 老师端',
    title: '老师课堂随机点名：录入名单、抽取学生与课堂加分',
    description:
      '点名星球是老师使用的课堂随机点名工具，支持班级名单粘贴或 Excel 导入、创建活动、随机抽取、课堂加分、共享老师和轮次管理。',
    lead:
      '老师创建活动并录入学生姓名后，就可以开始课堂随机点名。名单可直接多行粘贴，也可导入 Excel；抽取支持重复或本轮不重复。点名结果确认后可加 1 分，老师还能查看学生积分、等级和宠物成长。',
    facts: [
      ['快速录入班级名单', '直接粘贴多行学生姓名，或选择 .xlsx 文件导入；创建活动时可下载 Excel 模板。'],
      ['课堂随机点名', '可选择重复抽取或本轮不重复抽取，全部抽完后由老师手动开启新一轮。'],
      ['课堂激励', '每次结果可选择加 1 分或不加分，操作只会生效一次。'],
      ['协作教学', '创建者可以邀请其他老师共享活动、成员和点名轮次。'],
    ],
    questions: [
      ['如何用点名星球进行班级点名？', '注册并登录老师账号，在前台创建活动，粘贴学生姓名或导入 Excel 名单，选择是否允许重复点名后保存即可开始。'],
      ['支持 Excel 导入名单吗？', '支持 .xlsx 文件。可以下载模板后填写，也可导入已有名单；导入后可预览人数，并在保存活动时生效。'],
      ['老师能查看学生积分和宠物等级吗？', '可以。进入活动后，老师可查看本活动学生的积分、等级和宠物成长情况；宠物样式会在活动中为学生随机分配。'],
      ['共享老师可以做什么？', '共享老师可以共同编辑活动、管理名单、点名、加分和重置轮次；保存共享名单时会校验老师账号是否存在且已启用。'],
    ],
  },
  {
    slug: 'student',
    eyebrow: 'STUDENT GROWTH · 学生积分与成长',
    title: '老师查看每位学生的课堂积分与宠物等级',
    description:
      '老师可在活动中查看学生点名积分、等级和宠物成长。积分与宠物数据按活动分别记录，宠物样式在活动中随机分配。',
    lead: '点名星球当前由老师创建活动并管理课堂互动。老师在活动成员页查看每位学生的积分、等级和宠物成长进度；不同活动的数据相互独立。',
    facts: [
      ['活动内积分', '老师确认点名结果加分后，学生在对应活动中累计积分。'],
      ['宠物等级', '等级随活动积分成长，老师可在成员列表中查看等级和成长状态。'],
      ['活动独立记录', '同一学生参与不同活动时，积分和宠物成长分别记录，不会混在一起。'],
      ['随机分配宠物', '宠物样式在活动中随机分配，作为课堂参与成长的可视化反馈。'],
    ],
    questions: [
      ['老师在哪里查看学生宠物等级？', '登录老师账号并进入活动，在成员列表中即可查看学生当前积分、等级和宠物成长。'],
      ['同一个学生在不同活动中的积分会合并吗？', '不会。积分和宠物成长以学生与活动为单位分别记录。'],
    ],
  },
  {
    slug: 'random-roll-call',
    eyebrow: 'RANDOM ROLL CALL · 点名规则',
    title: '课堂随机点名怎么抽？重复与不重复规则说明',
    description:
      '了解课堂随机点名的重复抽取、不重复抽取、结果确认和轮次重置规则，选择适合班级互动的点名方式。',
    lead:
      '老师创建活动后可设置是否允许重复点名。关闭重复时，系统从本轮尚未抽中的学生中随机抽取；确认加分或不加分后才能继续，全部抽完可手动重置新一轮。',
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
    title: '多老师共享课堂点名活动：名单、轮次与结果同步',
    description:
      '点名星球支持老师通过账号共享课堂点名活动。共享老师可共同管理名单、随机点名、课堂加分和轮次状态。',
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
    title: '课堂随机点名常见问题：名单导入、点名规则与课堂加分',
    description:
      '点名星球课堂随机点名常见问题：老师账号注册、班级名单录入、Excel 导入、重复点名、课堂加分、学生积分等级和老师共享活动。',
    lead:
      '这里集中说明老师如何录入名单、设置点名规则、处理课堂积分、查看学生成长和共享活动。所有答案均对应当前版本的真实功能。',
    facts: [
      ['老师账号', '当前前台面向老师账号使用，注册并登录后可创建和管理自己的课堂活动。'],
      ['名单录入', '支持直接粘贴多行姓名，或导入 .xlsx 学生名单；活动中可再次编辑和导入。'],
      ['积分与成长', '老师处理点名结果时可选择加 1 分或不加分，并在活动成员中查看积分和宠物等级。'],
      ['活动共享', '输入其他老师账号并保存，校验通过后即可共享管理活动。'],
    ],
    questions: [
      ['如何导入班级名单？', '在创建或编辑活动时下载 Excel 模板填写后导入，也可以直接粘贴每行一个学生姓名。'],
      ['创建活动后还能修改名单吗？', '可以。老师可以再次编辑活动、增加或替换名单；系统会保留同名学生在该活动中的原积分和宠物成长。'],
      ['点名可以避免重复抽到同一人吗？', '可以。关闭“允许重复点名”后，当前轮次内每位学生最多被抽中一次，全部抽完后手动开启新一轮。'],
      ['老师如何查看学生成长？', '进入活动成员列表即可查看该活动内的学生积分、宠物和等级。'],
      ['老师能把活动共享给其他老师吗？', '可以。填写老师账号并保存，系统会验证账号存在且已启用。共享老师可共同管理活动。'],
      ['支持哪些使用方式？', '老师前台支持 H5 和微信小程序；未登录首页提供本地体验名单，体验数据不上传，也不累计正式积分。'],
    ],
  },
  {
    slug: 'about',
    eyebrow: 'ABOUT DIANMING PLANET · 关于我们',
    title: '让每一次参与，都被温柔看见。',
    description:
      '点名星球是一款面向老师的课堂随机点名工具，支持名单录入、Excel 导入、老师共享、课堂加分和学生成长查看。',
    lead:
      '点名星球从一个很小的课堂瞬间出发：当名字被点到，学生获得的不只是一次回答机会，也可以是一段持续可见的成长。',
    facts: [
      ['我们的目标', '降低老师组织课堂互动的成本，让随机点名更加公平、清楚和可追溯。'],
      ['成长而非排名', '活动记录用于课堂参与反馈和宠物成长，不鼓励公开比较学生。'],
      ['围绕老师课堂设计', '老师可在前台创建活动、管理名单、发起点名，并查看学生积分和宠物等级。'],
      ['持续改进', '产品围绕真实课堂流程设计，功能说明和规则会随版本及时更新。'],
    ],
    questions: [
      ['点名星球适合谁？', '适合希望通过随机提问、课堂加分和轻量游戏化成长提升学生参与感的老师、班级和培训课堂。'],
      ['它与普通随机抽签有什么不同？', '除了生成随机结果，点名星球还支持活动成员管理、共享轮次、结果确认、课堂积分以及活动内的学生宠物成长查看。'],
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
    <nav class="nav" aria-label="主要导航"><a href="/teacher/">老师点名工具</a><a href="/student/">积分与宠物等级</a><a href="/random-roll-call/">点名规则</a><a href="/faq/">常见问题</a><a class="login" href="/#/pages/login/login">老师登录 ↗</a></nav>
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

点名星球是一款面向老师的课堂随机点名工具，支持 H5 和微信小程序。
老师可以创建课堂活动，通过多行文本录入或 Excel 导入学生名单，进行重复或不重复随机点名、课堂加分，并邀请其他老师共享活动。
老师还可以在活动成员列表查看学生积分、等级和随机分配的宠物成长；未登录首页提供本地体验，体验名单不会上传或累计正式积分。

核心功能：
- 重复点名与当前轮次不重复点名
- 多行文本录入与 Excel 名单导入
- 多老师共享活动与点名轮次
- 每次结果加 1 分或不加分，且只能处理一次
- 老师查看活动内学生积分、等级与宠物成长
- 学生与活动维度独立保存积分与宠物

产品首页：${siteUrl}/
老师端说明：${siteUrl}/teacher/
学生积分与宠物等级说明（老师查看）：${siteUrl}/student/
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
