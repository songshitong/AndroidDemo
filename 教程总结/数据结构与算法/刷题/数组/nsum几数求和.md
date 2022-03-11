https://labuladong.gitee.io/algo/2/21/64/




Two Sum 系列问题在 LeetCode 上有好几道，这篇文章就挑出有代表性的几道，介绍一下这种问题怎么解决。

TwoSum I
https://leetcode-cn.com/problems/two-sum
这个问题的最基本形式是这样：给你一个数组和一个整数 target，可以保证数组中存在两个数的和为 target，请你返回这两个数的索引。
比如输入 nums = [3,1,3,6], target = 6，算法应该返回数组 [0,2]，因为 3 + 3 = 6。
这个问题如何解决呢？首先最简单粗暴的办法当然是穷举了：
```
int[] twoSum(int[] nums, int target) {

    for (int i = 0; i < nums.length; i++) 
        for (int j = i + 1; j < nums.length; j++) 
            if (nums[j] == target - nums[i]) 
                return new int[] { i, j };

    // 不存在这么两个数
    return new int[] {-1, -1};
}
```
这个解法非常直接，时间复杂度 O(N^2)，空间复杂度 O(1)。
可以通过一个哈希表减少时间复杂度：
```
int[] twoSum(int[] nums, int target) {
    int n = nums.length;
    HashMap<Integer, Integer> index = new HashMap<>();
    // 构造一个哈希表：元素映射到相应的索引
    for (int i = 0; i < n; i++)
        index.put(nums[i], i);
    
    for (int i = 0; i < n; i++) {
        int other = target - nums[i];
        // 如果 other 存在且不是 nums[i] 本身   存在两个相同元素=target的情况，需要校验index，第一个不是，第二个就是
        if (index.containsKey(other) && index.get(other) != i)
            return new int[] {i, index.get(other)};
    }
    
    return new int[] {-1, -1};
}
```
```
public int[] twoSum(int[] nums, int target) {
        int[] result = new int[]{-1,-1};
        HashMap<Integer,Integer> map = new HashMap();
        //也可以边存边判断   只有两个数，第一个不匹配，第二个就可以
        for(int i =0;i<nums.length;i++){
            if(!map.containsKey(nums[i])){
               map.put(nums[i],i); 
            }
            int other = target - nums[i];
            if(map.containsKey(other) && map.get(other)!=i){
                result[0]=i;
                result[1]=map.get(other);
                break;
            }
        }
        return result;
    }
```
这样，由于哈希表的查询时间为 O(1)，算法的时间复杂度降低到 O(N)，但是需要 O(N) 的空间复杂度来存储哈希表。不过综合来看，
  是要比暴力解法高效的。

我觉得 Two Sum 系列问题就是想教我们如何使用哈希表处理问题。我们接着往后看。


TwoSum II
https://leetcode-cn.com/problems/two-sum-iii-data-structure-design
这里我们稍微修改一下上面的问题。我们设计一个类，拥有两个 API：
```
class TwoSum {
    // 向数据结构中添加一个数 number
    public void add(int number);
    // 寻找当前数据结构中是否存在两个数的和为 value
    public boolean find(int value);
}
```
如何实现这两个 API 呢，我们可以仿照上一道题目，使用一个哈希表辅助 find 方法：
```
class TwoSum {
    Map<Integer, Integer> freq = new HashMap<>();

    public void add(int number) {
        // 记录 number 出现的次数
        freq.put(number, freq.getOrDefault(number, 0) + 1);
    }
    
    public boolean find(int value) {
        for (Integer key : freq.keySet()) {
            int other = value - key;
            // 情况一  添加了两个一样的元素，并且和为目标元素
            if (other == key && freq.get(key) > 1)
                return true;
            // 情况二  两个数不同
            if (other != key && freq.containsKey(other))
                return true;
        }
        return false;
    }
}
```
进行 find 的时候有两种情况，举个例子：
情况一：add 了 [3,3,2,5] 之后，执行 find(6)，由于 3 出现了两次，3 + 3 = 6，所以返回 true。
情况二：add 了 [3,3,2,5] 之后，执行 find(7)，那么 key 为 2，other 为 5 时算法可以返回 true。
除了上述两种情况外，find 只能返回 false 了。
对于这个解法的时间复杂度呢，add 方法是 O(1)，find 方法是 O(N)，空间复杂度为 O(N)，和上一道题目比较类似。
但是对于 API 的设计，是需要考虑现实情况的。比如说，我们设计的这个类，使用 find 方法非常频繁，那么每次都要 O(N) 的时间，
  岂不是很浪费费时间吗？对于这种情况，我们是否可以做些优化呢？

是的，对于频繁使用 find 方法的场景，我们可以进行优化。我们可以参考上一道题目的暴力解法，借助哈希集合来针对性优化 find 方法：
自己：适用于频繁find而add不频繁的场景
```
class TwoSum {
    Set<Integer> sum = new HashSet<>();
    List<Integer> nums = new ArrayList<>();

    public void add(int number) {
        // 记录所有可能组成的和
        for (int n : nums)
            sum.add(n + number);
        nums.add(number);
    }
    
    public boolean find(int value) {
        return sum.contains(value);
    }
}
```
这样 sum 中就储存了所有加入数字可能组成的和，每次 find 只要花费 O(1) 的时间在集合中判断一下是否存在就行了，显然非常适合频繁使用 find 的场景。

三、总结
对于 TwoSum 问题，一个难点就是给的数组无序。对于一个无序的数组，我们似乎什么技巧也没有，只能暴力穷举所有可能。

一般情况下，我们会首先把数组排序再考虑双指针技巧。TwoSum 启发我们，HashMap 或者 HashSet 也可以帮助我们处理无序数组相关的简单问题。

另外，设计的核心在于权衡，利用不同的数据结构，可以得到一些针对性的加强。

注意：TwoSum 要求返回数组的下标，需要排序算法是稳定的，即不改变元素下标

最后，如果 TwoSum I 中给的数组是有序的，应该如何编写算法呢？答案很简单，前文 双指针技巧汇总 写过：
```
int[] twoSum(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left < right) {
        int sum = nums[left] + nums[right];
        if (sum == target) {
            return new int[]{left, right};
        } else if (sum < target) {
            left++; // 让 sum 大一点
        } else if (sum > target) {
            right--; // 让 sum 小一点
        }
    }
    // 不存在这样两个数
    return new int[]{-1, -1};
}
```





https://labuladong.gitee.io/algo/1/13/
LeetCode 上面还有 3Sum，4Sum 问题，我估计以后出个 5Sum，6Sum 也不是不可能。
那么，对于这种问题有没有什么好办法用套路解决呢？
今天 labuladong 就由浅入深，层层推进，用一个函数来解决所有 nSum 类型的问题。
一、twoSum 问题
对于上面的twoSum, labuladong 要魔改一下题目，把这个题目变得更泛华，更困难一点。
题目告诉我们可以假设 nums 中有且只有一个答案，且需要我们返回对应元素的索引，现在修改这些条件：nums 中可能有多对儿元素之和都等于 target，
  请你的算法返回所有和为 target 的元素对儿，其中不能出现重复。
函数签名如下：
```
vector<vector<int>> twoSumTarget(vector<int>& nums, int target);
```
比如说输入为 nums = [1,3,1,2,2,3], target = 4，那么算法返回的结果就是：[[1,3],[2,2]]。
对于修改后的问题，返回元素的值而不是对应索引并没什么难度，关键难点是现在可能有多个和为 target 的数对儿，还不能重复，比如上述例子中
   [1,3] 和 [3,1] 就算重复，只能算一次。
首先，基本思路肯定还是排序加双指针：   
```
vector<vector<int>> twoSumTarget(vector<int>& nums, int target {
    // 先对数组排序
    sort(nums.begin(), nums.end());
    vector<vector<int>> res;
    int lo = 0, hi = nums.size() - 1;
    while (lo < hi) {
        int sum = nums[lo] + nums[hi];
        // 根据 sum 和 target 的比较，移动左右指针
        if      (sum < target) lo++;
        else if (sum > target) hi--;
        else {
            //sum==target,放入res保存
            res.push_back({lo, hi});
            lo++; hi--;
        }
    }
    return res;
}
```
但是，这样实现会造成重复的结果，比如说 nums = [1,1,1,2,2,3,3], target = 4，得到的结果中 [1,3] 肯定会重复。

出问题的地方在于 sum == target 条件的 if 分支，当给 res 加入一次结果后，lo 和 hi 不应该只改变 1，而应该跳过所有重复的元素
算法_刷题_nsum_1.jpeg
所以，可以对双指针的 while 循环做出如下修改：
```
while (lo < hi) {
    int sum = nums[lo] + nums[hi];
    // 记录索引 lo 和 hi 最初对应的值
    int left = nums[lo], right = nums[hi];
    if (sum < target)      lo++;
    else if (sum > target) hi--;
    else {
        res.push_back({left, right});
        // 跳过所有重复的元素
        while (lo < hi && nums[lo] == left) lo++;
        while (lo < hi && nums[hi] == right) hi--;
    }
}
```
这样就可以保证一个答案只被添加一次，重复的结果都会被跳过，可以得到正确的答案。不过，受这个思路的启发，其实前两个 if 分支也是可以做一点效率优化，
  跳过相同的元素：
```
vector<vector<int>> twoSumTarget(vector<int>& nums, int target) {
    // nums 数组必须有序
    sort(nums.begin(), nums.end());
    int lo = 0, hi = nums.size() - 1;
    vector<vector<int>> res;
    while (lo < hi) {
        int sum = nums[lo] + nums[hi];
        int left = nums[lo], right = nums[hi];
        if (sum < target) {
            while (lo < hi && nums[lo] == left) lo++;
        } else if (sum > target) {
            while (lo < hi && nums[hi] == right) hi--;
        } else {
            res.push_back({left, right});
            while (lo < hi && nums[lo] == left) lo++;
            while (lo < hi && nums[hi] == right) hi--;
        }
    }
    return res;
}
```

这样，一个通用化的 twoSum 函数就写出来了，请确保你理解了该算法的逻辑，我们后面解决 3Sum 和 4Sum 的时候会复用这个函数。
这个函数的时间复杂度非常容易看出来，双指针操作的部分虽然有那么多 while 循环，但是时间复杂度还是 O(N)，而排序的时间复杂度是 O(NlogN)，
  所以这个函数的时间复杂度是 O(NlogN)。

二、3Sum 问题
https://leetcode-cn.com/problems/3sum/
给 你 一 个 包含 n 个 整数 的 数组 nums ， 判 断 nums 中 是 否 存在 三 个 元 素 a，b，c ， 使 得 a+b+c=0? 请 你 找 出 所
有 满足 条 件 且 不 重复 的 三 元 组 。
注意 : 答案 中 不 可 以 包含 重复 的 三 元 组 。
题目就是让我们找 nums 中和为 0 的三个元素，返回所有可能的三元组（triple），函数签名如下：
```
vector<vector<int>> threeSum(vector<int>& nums);
```
这样，我们再泛化一下题目，不要光和为 0 的三元组了，计算和为 target 的三元组吧，同上面的 twoSum 一样，也不允许重复的结果：
```
vector<vector<int>> threeSum(vector<int>& nums) {
    // 求和为 0 的三元组
    return threeSumTarget(nums, 0);
}

vector<vector<int>> threeSumTarget(vector<int>& nums, int target) {
    // 输入数组 nums，返回所有和为 target 的三元组
}
```
这个问题怎么解决呢？很简单，穷举呗。现在我们想找和为 target 的三个数字，那么对于第一个数字，可能是什么？nums 中的每一个元素 nums[i] 都有可能！

那么，确定了第一个数字之后，剩下的两个数字可以是什么呢？其实就是和为 target - nums[i] 的两个数字呗，那不就是 twoSum 函数解决的问题么🤔

可以直接写代码了，需要把 twoSum 函数稍作修改即可复用：
```
/* 从 nums[start] 开始，计算有序数组
 * nums 中所有和为 target 的二元组 */
vector<vector<int>> twoSumTarget(
    vector<int>& nums, int start, int target) {
    // 左指针改为从 start 开始，其他不变
    int lo = start, hi = nums.size() - 1;
    vector<vector<int>> res;
    while (lo < hi) {
        ...
    }
    return res;
}

/* 计算数组 nums 中所有和为 target 的三元组 */
vector<vector<int>> threeSumTarget(vector<int>& nums, int target) {
    // 数组得排个序
    sort(nums.begin(), nums.end());
    int n = nums.size();
    vector<vector<int>> res;
    // 穷举 threeSum 的第一个数
    for (int i = 0; i < n; i++) {
        // 对 target - nums[i] 计算 twoSum
        vector<vector<int>> 
            tuples = twoSumTarget(nums, i + 1, target - nums[i]);
        // 如果存在满足条件的二元组，再加上 nums[i] 就是结果三元组
        for (vector<int>& tuple : tuples) {
            tuple.push_back(nums[i]);
            res.push_back(tuple);
        }
        // 跳过第一个数字重复的情况，否则会出现重复结果
        while (i < n - 1 && nums[i] == nums[i + 1]) i++;
    }
    return res;
}
```
需要注意的是，类似 twoSum，3Sum 的结果也可能重复，比如输入是 nums = [1,1,1,2,3], target = 6，结果就会重复。

关键点在于，不能让第一个数重复，至于后面的两个数，我们复用的 twoSum 函数会保证它们不重复。所以代码中必须用一个 while 循环来
  保证 3Sum 中第一个元素不重复。
至此，3Sum 问题就解决了，时间复杂度不难算，排序的复杂度为 O(NlogN)，twoSumTarget 函数中的双指针操作为 O(N)，threeSumTarget 函数
  在 for 循环中调用 twoSumTarget 所以总的时间复杂度就是 O(NlogN + N^2) = O(N^2)。


三、4Sum 问题
https://leetcode-cn.com/problems/4sum/
给 定 一 个 包含 n 个 整数 的 数组 nums 和 一 个 目标 值 target ， 判 断 nums 中 是 否 存在 四 个 元 素 a，b，c 和 d,使
得 a+b+c+d 的 值 与 target 相等 ? 找 出 所 有 满足 条 件 且 不 重复 的 四 元 组 。
注意 :
答案 中 不 可 以 包含 重复 的 四 元 组 。

函数签名如下：
```
vector<vector<int>> fourSum(vector<int>& nums, int target);
```
都到这份上了，4Sum 完全就可以用相同的思路：穷举第一个数字，然后调用 3Sum 函数计算剩下三个数，最后组合出和为 target 的四元组。
```
vector<vector<int>> fourSum(vector<int>& nums, int target) {
    // 数组需要排序
    sort(nums.begin(), nums.end());
    int n = nums.size();
    vector<vector<int>> res;
    // 穷举 fourSum 的第一个数
    for (int i = 0; i < n; i++) {
        // 对 target - nums[i] 计算 threeSum
        vector<vector<int>> 
            triples = threeSumTarget(nums, i + 1, target - nums[i]);
        // 如果存在满足条件的三元组，再加上 nums[i] 就是结果四元组
        for (vector<int>& triple : triples) {
            triple.push_back(nums[i]);
            res.push_back(triple);
        }
        // fourSum 的第一个数不能重复
        while (i < n - 1 && nums[i] == nums[i + 1]) i++;
    }
    return res;
}

/* 从 nums[start] 开始，计算有序数组
 * nums 中所有和为 target 的三元组 */
vector<vector<int>> 
    threeSumTarget(vector<int>& nums, int start, int target) {
        int n = nums.size();
        vector<vector<int>> res;
        // i 从 start 开始穷举，其他都不变
        for (int i = start; i < n; i++) {
            ...
        }
        return res;
```
这样，按照相同的套路，4Sum 问题就解决了，时间复杂度的分析和之前类似，for 循环中调用了 threeSumTarget 函数，所以总的时间复杂度
  就是 O(N^3)。


四、100Sum 问题？
在 LeetCode 上，4Sum 就到头了，但是回想刚才写 3Sum 和 4Sum 的过程，实际上是遵循相同的模式的。我相信你只要稍微修改一下 4Sum 的函数
  就可以复用并解决 5Sum 问题，然后解决 6Sum 问题……
那么，如果我让你求 100Sum 问题，怎么办呢？其实我们可以观察上面这些解法，统一出一个 nSum 函数：
//模板类似登阶梯，可以处理重复和多个结果，不断递归计算target-nums[i]  baseCase为2个元素和
```
/* 注意：调用这个函数之前一定要先给 nums 排序 */  结果返回的是元素
vector<vector<int>> nSumTarget(
    vector<int>& nums, int n, int start, int target) {
    int sz = nums.size();
    vector<vector<int>> res;
    // 至少是 2Sum，且数组大小不应该小于 n
    if (n < 2 || sz < n) return res;
    // 2Sum 是 base case
    if (n == 2) {
        // 双指针那一套操作
        int lo = start, hi = sz - 1;
        while (lo < hi) {
            int sum = nums[lo] + nums[hi];
            int left = nums[lo], right = nums[hi];
            if (sum < target) {
                //跳过重复  调价左指针小于右指针，元素仍然是那个
                while (lo < hi && nums[lo] == left) lo++;
            } else if (sum > target) {
                while (lo < hi && nums[hi] == right) hi--;
            } else {
                res.push_back({left, right});
                //目标相等时，跳过重复元素要移动左右指针
                while (lo < hi && nums[lo] == left) lo++;
                while (lo < hi && nums[hi] == right) hi--;
            }
        }
    } else {
        // n > 2 时，递归计算 (n-1)Sum 的结果   注意i的开始为start，跳过之前的重复
        for (int i = start; i < sz; i++) {
            vector<vector<int>> 
                sub = nSumTarget(nums, n - 1, i + 1, target - nums[i]);
            for (vector<int>& arr : sub) {
                // (n-1)Sum 加上 nums[i] 就是 nSum
                arr.push_back(nums[i]);
                res.push_back(arr);
            }
            //注意跳过重复元素
            while (i < sz - 1 && nums[i] == nums[i + 1]) i++;
        }
    }
    return res;
}
```
嗯，看起来很长，实际上就是把之前的题目解法合并起来了，n == 2 时是 twoSum 的双指针解法，n > 2 时就是穷举第一个数字，
  然后递归调用计算 (n-1)Sum，组装答案。
需要注意的是，调用这个 nSum 函数之前一定要先给 nums 数组排序，因为 nSum 是一个递归函数，如果在 nSum 函数里调用排序函数，
  那么每次递归都会进行没有必要的排序，效率会非常低。
比如说现在我们写 LeetCode 上的 4Sum 问题：
```
vector<vector<int>> fourSum(vector<int>& nums, int target) {
    sort(nums.begin(), nums.end());
    // n 为 4，从 nums[0] 开始计算和为 target 的四元组
    return nSumTarget(nums, 4, 0, target);
}
```
再比如 LeetCode 的 3Sum 问题，找 target == 0 的三元组：
```
vector<vector<int>> threeSum(vector<int>& nums) {
    sort(nums.begin(), nums.end());
    // n 为 3，从 nums[0] 开始计算和为 0 的三元组
    return nSumTarget(nums, 3, 0, 0);        
}
```
那么，如果让你计算 100Sum 问题，直接调用这个函数就完事儿了。


threeSum的Java版
/* 注意：调用这个函数之前一定要先给 nums 排序 */  结果返回的是元素
```
public List<List<Integer>> threeSum(int[] nums) {        
      Arrays.sort(nums);  
      return nSum(nums,3,0,0);
    }
    
    public List<List<Integer>> nSum(int[] nums,int n,int start,int target){
        int size = nums.length;
        List<List<Integer>> result=new ArrayList<List<Integer>>(n);
        // 至少是 2Sum，且数组大小不应该小于 n
        if(n<2||size<n) return result;
        if(n==2){
            int low= start;
            int heigh= size-1;
            //使用左右指针确定两个值
            while(low<heigh){
               int left = nums[low];
               int right = nums[heigh]; 
               int sum =  left+right;
               if(sum>target){
                //跳过重复  调价左指针小于右指针，元素仍然是那个
                   while(low<heigh && nums[heigh]==right){
                       heigh--;
                   }                   
               }else if(sum<target){
                    while(low<heigh && nums[low]==left){
                       low++;
                   }                                     
               }else{
                   List<Integer> list = new ArrayList(2);
                   list.add(left);
                   list.add(right);
                   result.add(list);     
                    //目标相等时，跳过重复元素要移动左右指针       
                    while(low<heigh&&nums[heigh]==right){
                       heigh--;
                   }
                    while(low<heigh&&nums[low]==left){
                       low++;
                   }   
               }
            }
            
        }else{
            // n > 2 时，递归计算 (n-1)Sum 的结果   注意i的开始为start，跳过之前的重复
            for(int i=start;i<size;i++){
                List<List<Integer>> tempSum = nSum(nums,n-1,i+1,target-nums[i]);
                for(int j=0;j<tempSum.size();j++){
                   // (n-1)Sum 加上 nums[i] 就是 nSum
                    tempSum.get(j).add(0,nums[i]);
                    result.add(tempSum.get(j));
                }
                 //注意跳过重复元素
                while(i<size-1 && nums[i]==nums[i+1]){
                    i++;
                }
            }
        }
        return result;
    }
```
