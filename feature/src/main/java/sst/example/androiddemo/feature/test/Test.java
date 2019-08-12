package sst.example.androiddemo.feature.test;

public class Test {

    //leetcode 1
    public static void main(String[] args) {
        int[] arry = new int[]{0,1,2,3,4};
        int[] result = twoSum(arry,7);
        String str = result[0]+" "+result[1];
        System.out.println(str);
    }
    public static  int[] twoSum(int[] nums, int target) {
        int[] result= new int[2];
        for(int i=0;i<nums.length;i++){
            System.out.println("循环1");
            for(int j=0;j<nums.length;j++){
                System.out.println("循环2");
                if( nums[i] + nums[j] == target && i!=j){
                    result[0] = i;
                    result[1] = j;
                    System.out.println("i "+i);
                    System.out.println("j "+j);
                    System.out.println(" 开始 break");
//                    break; 只跳出一层循环
                    return  result;
                }
            }
        }
        return result;

    }
}
