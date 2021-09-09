graphql
https://graphql.cn/learn/

GraphQL 是一个用于 API 的查询语言，是一个使用基于类型系统来执行查询的服务端运行时（类型系统由你的数据定义）。
GraphQL 并没有和任何特定数据库或者存储引擎绑定，而是依靠你现有的代码和数据支撑。
一个 GraphQL 服务是通过定义类型和类型上的字段来创建的，然后给每个类型上的每个字段提供解析函数

字段 fields  GraphQL 是关于请求对象上的特定字段
查询
{
  hero {
    name
  }
}
结果
{
  "data": {
    "hero": {
      "name": "R2-D2"
    }
  }
}

查询和其结果拥有几乎一样的结构。这是 GraphQL 最重要的特性，因为这样一来，你就总是能得到你想要的数据，而服务器也准确地知道客户端请求的字段
还有一点，上述查询是可交互的。也就是你可以按你喜欢来改变查询，然后看看新的结果

次级选择（sub-selection）
{
  hero {
    name
    # 查询可以有备注！
    friends {
      name
    }
  }
}


参数（Arguments）
{
  human(id: "1000") {
    name
    height(unit: FOOT)
  }
}
在 GraphQL 中，每一个字段和嵌套对象都能有自己的一组参数，从而使得 GraphQL 可以完美替代多次 API 获取请求。
甚至你也可以给 标量（scalar）字段传递参数，用于实现服务端的一次转换，而不用每个客户端分别转换。

别名（Aliases）
两个 hero 字段将会存在冲突  别名
{
  empireHero: hero(episode: EMPIRE) {
    name
  }
  jediHero: hero(episode: JEDI) {
    name
  }
}
即便结果中的字段与查询中的字段能够匹配，但是因为他们并不包含参数，你就没法通过不同参数来查询相同字段。
这便是为何你需要别名 —— 这可以让你重命名结果中的字段为任意你想到的名字

片段（Fragments）
假设我们的 app 有比较复杂的页面，将正反派主角及其友军分为两拨。你立马就能想到对应的查询会变得复杂，因为我们需要将一些字段重复至少一次 —— 两方各一次以作比较。
这就是为何 GraphQL 包含了称作片段的可复用单元。片段使你能够组织一组字段，然后在需要它们的的地方引入。
片段的概念经常用于将复杂的应用数据需求分割成小块，特别是你要将大量不同片段的 UI 组件组合成一个初始数据获取的时候
{
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  appearsIn
  friends {
    name
  }
}

片段可以访问查询或变更中声明的变量。详见 变量。
query HeroComparison($first: Int = 3) {
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  friendsConnection(first: $first) {
    totalCount
    edges {
      node {
        name
      }
    }
  }
}

操作名称（Operation name）生产中使用这些可以使我们代码减少歧义，可以省略
操作类型可以是 query、mutation 或 subscription，描述你打算做什么类型的操作。
操作类型是必需的，除非你使用查询简写语法，在这种情况下，你无法为操作提供名称或变量定义

操作名称是你的操作的有意义和明确的名称。它仅在有多个操作的文档中是必需的，但我们鼓励使用它，因为它对于调试和服务器端日志记录非常有用
query HeroNameAndFriends {
  hero {
    name
    friends {
      name
    }
  }
}


变量（Variables）
GraphQL 拥有一级方法将动态值提取到查询之外，然后作为分离的字典传进去。这些动态值即称为变量
变量的声明
1使用 $variableName 替代查询中的静态值。
2声明 $variableName 为查询接受的变量之一。
3将 variableName: value 通过传输专用（通常是 JSON）的分离的变量字典中

# { "graphiql": true, "variables": { "episode": JEDI } }
query HeroNameAndFriends($episode: Episode) {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
变量定义（Variable definitions）
变量前缀必须为 $，后跟其类型
所有声明的变量都必须是标量、枚举型或者输入对象类型。所以如果想要传递一个复杂对象到一个字段上，你必须知道服务器上其匹配的类型。
变量定义可以是可选的或者必要的。上例中，Episode 后并没有 !，因此其是可选的。但是如果你传递变量的字段要求非空参数，那变量一定是必要的。

默认变量（Default variables）
当所有变量都有默认值的时候，你可以不传变量直接调用查询
query HeroNameAndFriends($episode: Episode = "JEDI") {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}


指令（Directives） 使用变量动态地改变我们查询的结构
一个指令可以附着在字段或者片段包含的字段上，然后以任何服务端期待的方式来改变查询的执行。
GraphQL 的核心规范包含两个指令，其必须被任何规范兼容的 GraphQL 服务器实现所支持：
@include(if: Boolean) 仅在参数为 true 时，包含此字段。
@skip(if: Boolean) 如果参数为 true，跳过此字段。
指令在你不得不通过字符串操作来增减查询的字段时解救你。服务端实现也可以定义新的指令来添加新的特性。
query Hero($episode: Episode, $withFriends: Boolean!) {
  hero(episode: $episode) {
    name
    friends @include(if: $withFriends) {
      name
    }
  }
}

变更（Mutations） 一个改变服务端数据的方法。
mutation CreateReviewForEpisode($ep: Episode!, $review: ReviewInput!) {
  createReview(episode: $ep, review: $review) {
    stars
    commentary
  }
}
输入
{
  "ep": "JEDI",
  "review": {
    "stars": 5,
    "commentary": "This is a great movie!"
  }
}
结果
{
  "data": {
    "createReview": {
      "stars": 5,
      "commentary": "This is a great movie!"
    }
  }
}
注意 createReview 字段如何返回了新建的 review 的 stars 和 commentary 字段(花括号的)。这在变更已有数据时特别有用，
  例如，当一个字段自增的时候，我们可以在一个请求中变更并查询这个字段的新值
REST 中，任何请求都可能最后导致一些服务端副作用，但是约定上建议不要使用 GET 请求来修改数据。
GraphQL 也是类似 —— 技术上而言，任何查询都可以被实现为导致数据写入。然而，建一个约定来规范任何导致写入的操作都应该显式通过变更（mutation）来发送。

变更中的多个字段（Multiple fields in mutations）
一个变更也能包含多个字段，一如查询。查询和变更之间名称之外的一个重要区别是：

查询字段时，是并行执行，而变更字段时，是线性执行，一个接着一个。

这意味着如果我们一个请求中发送了两个 incrementCredits 变更，第一个保证在第二个之前执行，以确保我们不会出现竞态。


内联片段（Inline Fragments） GraphQL schema 也具备定义接口和联合类型的能力
如果你查询的字段返回的是接口或者联合类型，那么你可能需要使用内联片段来取出下层具体类型的数据：
query HeroForEpisode($ep: Episode!) {
  hero(episode: $ep) {
    name
    ... on Droid {
      primaryFunction
    }
    ... on Human {
      height
    }
  }
}
输出
{
  "data": {
    "hero": {
      "name": "R2-D2",
      "primaryFunction": "Astromech"
    }
  }
}

因为第一个片段标注为 ... on Droid，primaryFunction 仅在 hero 返回的 Character 为 Droid 类型时才会执行。同理适用于 Human 类型的 height 字段


元字段（Meta fields）
某些情况下，你并不知道你将从 GraphQL 服务获得什么类型，这时候你就需要一些方法在客户端来决定如何处理这些数据。GraphQL 允许你在查询的任何位置请求 __typename，一个元字段，以获得那个位置的对象类型名称
{
  search(text: "an") {
    __typename
    ... on Human {
      name
    }
    ... on Droid {
      name
    }
    ... on Starship {
      name
    }
  }
}
结果：
{
  "data": {
    "search": [
      {
        "__typename": "Human",
        "name": "Han Solo"
      },
      {
        "__typename": "Human",
        "name": "Leia Organa"
      },
      {
        "__typename": "Starship",
        "name": "TIE Advanced x1"
      }
    ]
  }
}
上面的查询中，search 返回了一个联合类型，其可能是三种选项之一。没有 __typename 字段的情况下，几乎不可能在客户端分辨开这三个不同的类型

Schema 和类型
类型系统（Type System） #
因为一个 GraphQL 查询的结构和结果非常相似，因此即便不知道服务器的情况，你也能预测查询会返回什么结果。
但是一个关于我们所需要的数据的确切描述依然很有意义，我们能选择什么字段？服务器会返回哪种对象？这些对象下有哪些字段可用？这便是引入 schema 的原因。

每一个 GraphQL 服务都会定义一套类型，用以描述你可能从那个服务查询到的数据。每当查询到来，服务器就会根据 schema 验证并执行查询

类型语言（Type Language） #
GraphQL 服务可以用任何语言编写，因为我们并不依赖于任何特定语言的句法句式（譬如 JavaScript）来与 GraphQL schema 沟通，
我们定义了自己的简单语言，称之为 “GraphQL schema language” —— 它和 GraphQL 的查询语言很相似，让我们能够和 GraphQL schema 之间可以无语言差异地沟通



最佳实践
HTTP
GraphQL 通常通过单入口来提供 HTTP 服务的完整功能，这一实现方式与暴露一组 URL 且每个 URL 只暴露一个资源的 REST API 不同。
虽然 GraphQL 也可以暴露多个资源 URL 来使用，但这可能导致您在使用 GraphiQL 等工具时遇到困难

json(使用Gzip压缩)
Accept-Encoding: gzip

版本控制
虽然没有什么可以阻止 GraphQL 服务像任何其他 REST API 一样进行版本控制，但 GraphQL 强烈认为可以通过 GraphQL schema 的持续演进来避免版本控制。

为什么大多数 API 有版本？当某个 API 入口能够返回的数据被限制，则任何更改都可以被视为一个破坏性变更，而破坏性变更需要发布一个新的版本。
 如果向 API 添加新功能需要新版本，那么在经常发布版本并拥有许多增量版本与保证 API 的可理解性和可维护性之间就需要权衡。

相比之下，GraphQL 只返回显式请求的数据，因此可以通过增加新类型和基于这些新类型的新字段添加新功能，而不会造成破坏性变更。这样可以衍生出始终避免破坏性变更并提供无版本 API 的通用做法


可以为空的性质
大多数能够识别 “null” 的类型系统都提供普通类型和该类型可以为空的版本，默认情况下，类型不包括 “null”，除非明确声明。但在 GraphQL 类型系统中，默认情况下每个字段都可以为空。这是因为在由数据库和其他服务支持的联网服务中可能会出现许多问题，比如数据库可能会宕机，异步操作可能会失败，异常可能会被抛出。除了系统故障之外，授权通常可以是细粒度的，请求中的各个字段可以具有不同的授权规则。

通过默认设置每个字段可以为空，以上任何原因都可能导致该字段返回 “null”，而不是导致请求完全失败。作为替代，GraphQL 提供 non-null 这一变体类型来保证当客户端发出请求时，该字段永远不会返回 “null”。相反，如果发生错误，则上一个父级字段将为 “null”。

在设计 GraphQL schema 时，请务必考虑所有可能导致错误的情况下，“null” 是否可以作为获取失败的字段合理的返回值。通常它是，但偶尔，它不是。在这种情况下，请使用非空类型进行保证


分页
GraphQL 类型系统允许某些字段返回 值的列表，但是为长列表分页的功能则交给 API 设计者自行实现。为 API 设计分页功能有很多种各有利弊的方案。

通常当字段返回长列表时，可以接收参数 “first” 和 “after” 来指定列表的特定区域，其中 “after” 是列表中每个值的唯一标识符

分页和边
我们有很多种方法来实现分页：

我们可以像这样 friends(first:2 offset:2) 来请求列表中接下来的两个结果。
我们可以像这样 friends(first:2 after:$friendId), 来请求我们上一次获取到的最后一个朋友之后的两个结果。
我们可以像这样 friends(first:2 after:$friendCursor), 从最后一项中获取一个游标并使用它来分页。
一般来说，我们发现基于游标的分页是最强大的分页。特别当游标是不透明的时，则可以使用基于游标的分页（通过为游标设置偏移或 ID）来实现基于偏移或基于 ID 的分页，并且如果分页模型在将来发生变化，则使用游标可以提供额外的灵活性。需要提醒的是，游标是不透明的，并且它们的格式不应该被依赖，我们建议用 base64 编码它们。

这导致我们遇到一个问题：我们如何从对象中获取游标？我们不希望游标放置在 User 类型上；它是连接的属性，而不是对象的属性。所以我们可能想要引入一个新的间接层；我们的 friends 字段应该给我们一个边（edge）的列表，边同时具有游标和底层节点：
{
  hero {
    name
    friends(first:2) {
      edges {
        node {
          name
        }
        cursor
      }
    }
  }
}
如果存在针对于边而不是针对于某一个对象的信息，则边这个概念也被证明是有用的。例如，如果我们想要在 API 中暴露“友谊时间”，将其放置在边里是很自然的。

列表的结尾、计数以及连接
现在我们有能力使用游标对连接进行分页，但是我们如何知道何时到达连接的结尾？我们必须继续查询，直到我们收到一个空列表，但是我们真的希望连接能够告诉我们什么时候到达结尾，这样我们不需要额外的请求。同样的，如果我们想知道关于连接本身的附加信息怎么办；例如，R2-D2 有多少个朋友？

为了解决这两个问题，我们的 friends 字段可以返回一个连接对象。连接对象将拥有一个存放边的字段以及其他信息（如总计数和有关下一页是否存在的信息）。所以我们的最终查询可能看起来像这样
{
  hero {
    name
    friends(first:2) {
      totalCount
      edges {
        node {
          name
        }
        cursor
      }
      pageInfo {
        endCursor
        hasNextPage
      }
    }
  }
}

文件上传
mutation SingleUpload($file: Upload!) {
  singleUpload(file: $file) {
    id
    path
    filename
    mimetype
  }
}