https://labuladong.gitee.io/algo/2/19/35/

https://leetcode-cn.com/problems/course-schedule/
https://leetcode-cn.com/problems/course-schedule-ii/

拓扑
拓扑是研究几何图形或空间在连续改变形状后还能保持不变的一些性质的一个学科。它只考虑物体间的位置关系而不考虑它们的形状和大小。
拓扑英文名是Topology，直译是地志学，最早指研究地形、地貌相类似的有关学科。几何拓扑学是十九世纪形成的一门数学分支，它属于几何学的范畴。
比如一个树忽略树的颜色，叶子，品种，可以抽象为点线组成的多叉树

很多读者留言说要看「图」相关的算法，那就满足大家，结合算法题把图相关的技巧给大家过一遍。
前文 学习数据结构的框架思维 说了，数据结构相关的算法无非两点：遍历 + 访问。那么图的基本遍历方法也很简单，前文 图算法基础 就讲了
  如何从多叉树的遍历框架扩展到图的遍历。
图这种数据结构还有一些比较特殊的算法，比如二分图判断，有环图无环图的判断，拓扑排序，以及最经典的最小生成树，单源最短路径问题，
  更难的就是类似网络流这样的问题。
不过以我的经验呢，像网络流这种问题，你又不是打竞赛的，除非自己特别有兴趣，否则就没必要学了；像最小生成树和最短路径问题，
  虽然从刷题的角度用到的不多，但它们属于经典算法，学有余力可以掌握一下；像拓扑排序这一类，属于比较基本且有用的算法，应该比较熟练地掌握。

那么本文就结合具体的算法题，来说两个图论算法：有向图的环检测、拓扑排序算法。


判断有向图是否存在环
先来看看力扣第 207 题「课程表」：
你这个学期必须选修 numCourses 门课程，记为0到numCourses - 1 。
在选修某些课程之前需要一些先修课程。 先修课程按数组prerequisites 给出，其中prerequisites[i] = [ai, bi] ，表示如果要学习课程ai 
  则 必须 先学习课程 bi 。
例如，先修课程对[0, 1] 表示：想要学习课程 0 ，你需要先完成课程 1 。
请你判断是否可能完成所有课程的学习？如果可以，返回 true ；否则，返回 false 。
示例 1：
输入：numCourses = 2, prerequisites = [[1,0]]
输出：true
解释：总共有 2 门课程。学习课程 1 之前，你需要完成课程 0 。这是可能的。
示例 2：
输入：numCourses = 2, prerequisites = [[1,0],[0,1]]
输出：false
解释：总共有 2 门课程。学习课程 1 之前，你需要先完成课程 0 ；并且学习课程 0 之前，你还应先完成课程 1 。这是不可能的。
函数签名
```
int[] findOrder(int numCourses, int[][] prerequisites);
```

题目应该不难理解，什么时候无法修完所有课程？当存在循环依赖的时候。
其实这种场景在现实生活中也十分常见，比如我们写代码 import 包也是一个例子，必须合理设计代码目录结构，否则会出现循环依赖，编译器会报错，
   所以编译器实际上也使用了类似算法来判断你的代码是否能够成功编译。

看到依赖问题，首先想到的就是把问题转化成「有向图」这种数据结构，只要图中存在环，那就说明存在循环依赖。
具体来说，我们首先可以把课程看成「有向图」中的节点，节点编号分别是 0, 1, ..., numCourses-1，把课程之间的依赖关系看做节点之间的有向边。
比如说必须修完课程 1 才能去修课程 3，那么就有一条有向边从节点 1 指向 3。
所以我们可以根据题目输入的 prerequisites 数组生成一幅类似这样的图：
算法_刷题_图算法_拓扑排序_环检测1.jpeg

如果发现这幅有向图中存在环，那就说明课程之间存在循环依赖，肯定没办法全部上完；反之，如果没有环，那么肯定能上完全部课程。
好，那么想解决这个问题，首先我们要把题目的输入转化成一幅有向图，然后再判断图中是否存在环。

如何转换成图呢？我们前文 图论基础 写过图的两种存储形式，邻接矩阵和邻接表。
以我刷题的经验，常见的存储方式是使用邻接表，比如下面这种结构：
```
List<Integer>[] graph;
```
graph[s] 是一个列表，存储着节点 s 所指向的节点。
所以我们首先可以写一个建图函数：
```
List<Integer>[] buildGraph(int numCourses, int[][] prerequisites) {
    // 图中共有 numCourses 个节点
    List<Integer>[] graph = new LinkedList[numCourses];
    for (int i = 0; i < numCourses; i++) {
        graph[i] = new LinkedList<>();
    }
    for (int[] edge : prerequisites) {
        int from = edge[1];
        int to = edge[0];
        // 修完课程 from 才能修课程 to
        // 在图中添加一条从 from 指向 to 的有向边
        graph[from].add(to);
    }
    return graph;
}
```

图建出来了，怎么判断图中有没有环呢？
先不要急，我们先来思考如何遍历这幅图，只要会遍历，就可以判断图中是否存在环了。
前文 图论基础 写了 DFS 算法遍历图的框架，无非就是从多叉树遍历框架扩展出来的，加了个 visited 数组罢了：
```
// 防止重复遍历同一个节点
boolean[] visited;
// 从节点 s 开始 DFS 遍历，将遍历过的节点标记为 true
void traverse(List<Integer>[] graph, int s) {
    if (visited[s]) {
        return;
    }
    /* 前序遍历代码位置 */
    // 将当前节点标记为已遍历
    visited[s] = true;
    for (int t : graph[s]) {
        traverse(graph, t);
    }
    /* 后序遍历代码位置 */
}
```

那么我们就可以直接套用这个遍历代码：
```
// 防止重复遍历同一个节点
boolean[] visited;

boolean canFinish(int numCourses, int[][] prerequisites) {
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);
    
    visited = new boolean[numCourses];
    //从0到numCourses 遍历存在的课程
    for (int i = 0; i < numCourses; i++) {
        traverse(graph, i);
    }
}

void traverse(List<Integer>[] graph, int s) {
    // 代码见上文
}
```

注意图中并不是所有节点都相连，所以要用一个 for 循环将所有节点都作为起点调用一次 DFS 搜索算法。
这样，就能遍历这幅图中的所有节点了，你打印一下 visited 数组，应该全是 true。
前文 学习数据结构和算法的框架思维 说过，图的遍历和遍历多叉树差不多，所以到这里你应该都能很容易理解。

现在可以思考如何判断这幅图中是否存在环。
我们前文 回溯算法核心套路详解 说过，你可以把递归函数看成一个在递归树上游走的指针，这里也是类似的：
你也可以把 traverse 看做在图中节点上游走的指针，只需要再添加一个布尔数组 onPath 记录当前 traverse 经过的路径：
```
boolean[] onPath;

boolean hasCycle = false;
boolean[] visited;

void traverse(List<Integer>[] graph, int s) {
    if (onPath[s]) {
        // 发现环！！！
        hasCycle = true;
    }
    if (visited[s]) {
        return;
    }
    // 将节点 s 标记为已遍历
    visited[s] = true;
    // 开始遍历节点 s
    onPath[s] = true;
    for (int t : graph[s]) {
        traverse(graph, t);
    }
    // 节点 s 遍历完成
    onPath[s] = false;
}
```

这里就有点回溯算法的味道了，在进入节点 s 的时候将 onPath[s] 标记为 true，离开时标记回 false，如果发现 onPath[s] 已经被标记，说明出现了环。
PS：参考贪吃蛇没绕过弯儿咬到自己的场景。
这样，就可以在遍历图的过程中顺便判断是否存在环了，完整代码如下：
```
// 记录一次 traverse 递归经过的节点
boolean[] onPath;
// 记录遍历过的节点，防止走回头路
boolean[] visited;
// 记录图中是否有环
boolean hasCycle = false;

boolean canFinish(int numCourses, int[][] prerequisites) {
    //list 数组
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);
    
    visited = new boolean[numCourses];
    onPath = new boolean[numCourses];
    
    //可能不是所有的节点都在图结构中，需要遍历所有节点
    for (int i = 0; i < numCourses; i++) {
        // 遍历图中的所有节点
        traverse(graph, i);
    }
    // 只要没有循环依赖可以完成所有课程
    return !hasCycle;
}

void traverse(List<Integer>[] graph, int s) {
    //注意标记环是onPath[s]为true， a->b->a  同一条路径,所以遍历完一条路径opPath[s]重置为false
    //    visited[s]不一定是环，a->c,b->c,c访问两次但不是环 
    if (onPath[s]) {
        // 出现环
        hasCycle = true;
    }
    
    if (visited[s] || hasCycle) {
        // 如果已经找到了环，也不用再遍历了
        return;
    }
    // 前序遍历代码位置
    visited[s] = true;
    onPath[s] = true;
    for (int t : graph[s]) {
        traverse(graph, t);
    }
    // 后序遍历代码位置
    onPath[s] = false;
}

List<Integer>[] buildGraph(int numCourses, int[][] prerequisites) {
    // 图中共有 numCourses 个节点
    List<Integer>[] graph = new LinkedList[numCourses];
    for (int i = 0; i < numCourses; i++) {
        graph[i] = new LinkedList<>();
    }
    for (int[] edge : prerequisites) {
        int from = edge[1];
        int to = edge[0];
        // 修完课程 from 才能修课程 to   元素与index相同，可以直接用
        // 在图中添加一条从 from 指向 to 的有向边
        graph[from].add(to);
    }
    return graph;
}
```
这道题就解决了，核心就是判断一幅有向图中是否存在环。


去掉onPath 可以吗   不可以
visited 记录哪些节点被遍历过，而 onPath 记录当前递归堆栈中有哪些节点，它们的作用不同，所以并不重复
canFinish(4,new int[][]{{1,0},{2,0},{3,1},{3,2}})
函数调用完，onPath的结果  [false, false, false, false]  onpath记录路径相关，路径重合存在环
 visited结果[true, true, true, true]        visited用来保证节点只被访问一次，变量一旦置为true就不变了

visited数组是用来剪枝的，在代码中是全局变量，并且只赋值true，而没有像onPath一样有回溯操作（即onPath【s】=false），举个例值，
 有a，b两个节点都有一条到c的路径，那么a判断之后被标记了visited【s】=true，b再去遍历的时候可以直接返回。
onPath是记录回溯的路径的，是检查环是否存在的重要标志！
经过测试，只有OnPath没有visited，在100个节点的时候就会超时。

visited还可以改造成int数组类型，这样可以用-1代表其他遍历访问过，1代表本轮访问过，0代表还未访问。从而去掉onPath数组
visited 记录走过的路径，onPath 记录递归堆栈的路径，类比贪吃蛇，贪吃蛇经过的地方 visited 都会被标记成 true，而 onPath 就只是记录贪吃蛇的身子



不过如果出题人继续恶心你，让你不仅要判断是否存在环，还要返回这个环具体有哪些节点，怎么办？
你可能说，onPath 里面为 true 的索引，不就是组成环的节点编号吗？
不是的，假设下图中绿色的节点是递归的路径，它们在 onPath 中的值都是 true，但显然成环的节点只是其中的一部分：
算法_刷题_图算法_拓扑排序_环检测2.jpeg
这个问题留给大家思考，我会在公众号留言区置顶正确的答案。
输出环的结点可以参考tarjan算法，每访问一个结点就将其入全局栈，如果发现下一个要访问的元素在栈里面，就一直出栈直到该结点。
这是一种方法，inStack 布尔数组结合全局栈，可以找到环元素，已置顶
也可以不维护一个显式的栈，直接利用递归函数就在栈里面，维护一个全局变量，访问到了正在访问的元素就把这个全局变量置为该元素的序号，
  然后一路回退，边回退边输出结点，直到访问到该全局变量标记的结点。

那么接下来，我们来再讲一个经典的图算法：拓扑排序。
拓扑排序
看下力扣第 210 题「课程表 II」：
现在你总共有 numCourses 门课需要选，记为0到numCourses - 1。给你一个数组prerequisites ，其中 prerequisites[i] = [ai, bi] ，
  表示在选修课程 ai 前 必须 先选修bi 。
例如，想要学习课程 0 ，你需要先完成课程1 ，我们用一个匹配来表示：[0,1] 。
返回你为了学完所有课程所安排的学习顺序。可能会有多个正确的顺序，你只要返回 任意一种 就可以了。如果不可能完成所有课程，返回 一个空数组 。
示例 2：
输入：numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]
输出：[0,2,1,3]
解释：总共有 4 门课程。要学习课程 3，你应该先完成课程 1 和课程 2。并且课程 1 和课程 2 都应该排在课程 0 之后。
因此，一个正确的课程顺序是[0,1,2,3] 。另一个正确的排序是[0,2,1,3] 。

这道题就是上道题的进阶版，不是仅仅让你判断是否可以完成所有课程，而是进一步让你返回一个合理的上课顺序，保证开始修每个课程时，前置的课程都已经修完。
函数签名如下：
```
int[] findOrder(int numCourses, int[][] prerequisites);
```

这里我先说一下拓扑排序（Topological Sorting）这个名词，网上搜出来的定义很数学，这里干脆用百度百科的一幅图来让你直观地感受下：
算法_刷题_图算法_拓扑排序1.jpg

直观地说就是，让你把一幅图「拉平」，而且这个「拉平」的图里面，所有箭头方向都是一致的，比如上图所有箭头都是朝右的。
很显然，如果一幅有向图中存在环，是无法进行拓扑排序的，因为肯定做不到所有箭头方向一致；反过来，如果一幅图是「有向无环图」，那么一定可以进行拓扑排序。

但是我们这道题和拓扑排序有什么关系呢？
其实也不难看出来，如果把课程抽象成节点，课程之间的依赖关系抽象成有向边，那么这幅图的拓扑排序结果就是上课顺序。
首先，我们先判断一下题目输入的课程依赖是否成环，成环的话是无法进行拓扑排序的，所以我们可以复用上一道题的主函数：
```
public int[] findOrder(int numCourses, int[][] prerequisites) {
    if (!canFinish(numCourses, prerequisites)) {
        // 不可能完成所有课程
        return new int[]{};
    }
    // ...
}
```


那么关键问题来了，如何进行拓扑排序？是不是又要秀什么高大上的技巧了？
其实特别简单，将后序遍历的结果进行反转，就是拓扑排序的结果。
直接看解法代码吧，在上一题环检测的代码基础上添加了记录后序遍历结果的逻辑：
```
// 记录后序遍历结果
List<Integer> postorder = new ArrayList<>();
// 记录是否存在环
boolean hasCycle = false;
boolean[] visited, onPath;

// 主函数
public int[] findOrder(int numCourses, int[][] prerequisites) {
    List<Integer>[] graph = buildGraph(numCourses, prerequisites);
    visited = new boolean[numCourses];
    onPath = new boolean[numCourses];
    // 遍历图
    for (int i = 0; i < numCourses; i++) {
        traverse(graph, i);
    }
    // 有环图无法进行拓扑排序
    if (hasCycle) {
        return new int[]{};
    }
    // 逆后序遍历结果即为拓扑排序结果
    Collections.reverse(postorder);
    int[] res = new int[numCourses];
    for (int i = 0; i < numCourses; i++) {
        res[i] = postorder.get(i);
    }
    return res;
}

// 图遍历函数
void traverse(List<Integer>[] graph, int s) {
    if (onPath[s]) {
        // 发现环
        hasCycle = true;
    }
    if (visited[s] || hasCycle) {
        return;
    }
    // 前序遍历位置
    onPath[s] = true;
    visited[s] = true;
    for (int t : graph[s]) {
        traverse(graph, t);
    }
    // 后序遍历位置
    postorder.add(s);
    onPath[s] = false;
}

// 建图函数
List<Integer>[] buildGraph(int numCourses, int[][] prerequisites) {
    // 图中共有 numCourses 个节点
    List<Integer>[] graph = new LinkedList[numCourses];
    for (int i = 0; i < numCourses; i++) {
        graph[i] = new LinkedList<>();
    }
    for (int[] edge : prerequisites) {
        int from = edge[1];
        int to = edge[0];
        // 修完课程 from 才能修课程 to
        // 在图中添加一条从 from 指向 to 的有向边
        graph[from].add(to);
    }
    return graph;
}
```


代码虽然看起来多，但是逻辑应该是很清楚的，只要图中无环，那么我们就调用 traverse 函数对图进行 DFS 遍历，记录后序遍历结果，
  最后把后序遍历结果反转，作为最终的答案。

那么为什么后序遍历的反转结果就是拓扑排序呢？
我这里也避免数学证明，用一个直观地例子来解释，我们就说二叉树，这是我们说过很多次的二叉树遍历框架：
```
void traverse(TreeNode root) {
    // 前序遍历代码位置
    traverse(root.left)
    // 中序遍历代码位置
    traverse(root.right)
    // 后序遍历代码位置
}
```
二叉树的后序遍历是什么时候？遍历完左右子树之后才会执行后序遍历位置的代码。换句话说，当左右子树的节点都被装到结果列表里面了，根节点才会被装进去。
后序遍历的这一特点很重要，之所以拓扑排序的基础是后序遍历，是因为一个任务必须在等到所有的依赖任务都完成之后才能开始开始执行。
你把每个任务理解成二叉树里面的节点，这个任务所依赖的任务理解成子节点，那你是不是应该先把所有子节点处理完再处理父节点？这是不是就是后序遍历？
算法_刷题_图算法_拓扑排序2.jpeg

再说一说为什么还要把后序遍历结果反转，才是最终的拓扑排序结果。
我们说一个节点可以理解为一个任务，这个节点的子节点理解为这个任务的依赖，但你注意我们之前说的依赖关系的表示：如果做完 A 才能去做 B，
  那么就有一条从 A 指向 B 的有向边，表示 B 依赖 A。
那么，父节点依赖子节点，体现在二叉树里面应该是这样的：
算法_刷题_图算法_拓扑排序3.jpeg

是不是和我们正常的二叉树指针指向反过来了？所以正常的后序遍历结果应该进行反转，才是拓扑排序的结果。
以上，我简单解释了一下为什么「拓扑排序的结果就是反转之后的后序遍历结果」，当然，我的解释虽然比较直观，但并没有严格的数学证明，有兴趣的读者可以自己查一下。
//todo 
总之，你记住拓扑排序就是后序遍历反转之后的结果，且拓扑排序只能针对有向无环图，进行拓扑排序之前要进行环检测，这些知识点已经足够了。



