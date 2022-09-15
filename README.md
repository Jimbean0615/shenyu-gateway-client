# Shenyu Dubbo Consumer Client

## 一、项目简介
* 通过扫描注解自动生成dubbo客户端接口实现代码，从而较低的成本将dubbo请求转发到网关上。

## 二、项目功能
- [x] 自动生成dubbo接口实现类
- [x] 自动对接shenyu网关签名，透传上下文信息等

## 三、使用说明

### 1. 引入依赖包
```xml
<dependency>
    <groupId>com.jimbean.shenyu.client</groupId>
    <artifactId>shenyu-gateway-client-starter</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 2. 增加配置项
```yaml
gateway:
  endpoint: http://localhost:9195
  sign:
    enabled: true
    appKey: 6089234C06C44A7A906FDD611A379FF7
    appSecret: 6459ED0B8D4C4E4FB20D05479203EDC0
```

### 3. 启动类上添加`@DubboGatewayScanner`注解
```java
@SpringBootApplication
@DubboGatewayScanner(basePackages = "com.jimbean.examples.dubbo.api")
public class TestApacheDubboConsumerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(TestApacheDubboConsumerApplication.class, args);
    }
}
```

### 4. 调用dubbo接口

> 像调用本地Spring bean一样，无需引入dubbo依赖，更无需配置dubbo注册中心等信息

```java
@RestController
@RequestMapping("/test")
public class HttpTestController {
    
    @Resource
    private DubboAnnotationService dubboAnnotationService;

    @GetMapping("/findAll")
    public DubboTest findAll() {
        return dubboAnnotationService.findAll();
    }

    @PostMapping("/saveOrUpdate")
    public DubboTest saveOrUpdate(@RequestBody RequestDto requestDto) {
        return dubboAnnotationService.saveOrUpdate(requestDto);
    }
}
```


## 四、原理说明

项目启动时自动扫描dubbo接口，通过javassist动态字节码技术生成dubbo接口实现类

注解说明
- `@DubboGatewayScanner` 配置dubbo接口扫描路径，并引导`DubboGatewayImportBeanDefinitionRegistrar`类注入接口实现类
- `@Gateway` 配置dubbo转成http协议后的contextPath
- `@ShenyuDubboClient` shenyu网关dubbo核心注解，判断接口uri路径

