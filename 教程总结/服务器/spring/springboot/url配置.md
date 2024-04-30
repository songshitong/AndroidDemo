
https://www.cnblogs.com/nelson-hu/p/6595522.html
@requestMapping： 类级别和方法级别的注解， 指明前后台解析的路径。 有value属性(一个参数时默认)指定url路径解析，method属性指定提交方式(默认为get提交) 
```
@RequestMapping(value = "/testing")
public class QuestionSetDisplayController extends BaseController {}
```

get请求
```
    @GetMapping("")
    public List<DefinitionDTO> query(
            @RequestParam(value="type") String type) throws Exception {
       ...
    }
```
post请求
```
    @PostMapping("/batchDelete")
    public void batchDelete(@RequestBody VehicleDTO vehicle) throws Exception {
      ...
    }
```

请求参数规则注解   RequestParam中name和value是等价的
```
@RequestMapping("/login")  // /login?name=tom&age=20
   public String login(@RequestParam(value="age",required=false,defaultValue="24") String agenum,@RequestParam("name") String name){
      ....
   }
```

@PathVariable: url参数注解， 一般用于从url中获取参数
```
@RequestMapping(value = "/system/getAllCodeTableData/{category}", method = RequestMethod.GET) 
//前台url： '/system/getAllCodeTableData/APPLICANT_ENGLISH'　　
public List<CodeTableModel> getCodeTableModelByCategory(@PathVariable String category) throws OTPException {
　return codeTableService.getCodeTableModelByCategory(category); 
}
```