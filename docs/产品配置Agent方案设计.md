#产品配置Agent设计文档

## 一、概述
### 1.1 设计目标
   设计一个产品配置agent，BS架构
### 1.2 核心功能
- 根据用户输入，识别配置需求
- 对配置需求中产品规格需求进行标准化解析
- 根据标准化的配置规格需求对照每个产品的规格，计算产品的偏离度
- 根据产品的总偏离度进行排序，挑选最大满足度的产品（产品选型）
- 对选择的产品，根据标准化规格进行参数配置
--总结输出


## 二、架构设计
    BS架构
### 1.前后端接口：

1.1创建会话接口 (POST)
POST /api/v1/sessions
Content-Type: application/json
Authorization: Bearer <token>

req:

{
  "user_input": "我想审核订单12345",
  "context": {
    "user_id": "user_001",
    "user_role": "manager"
  }
}
rsp: Session

1.2. 继续会话接口 (POST)
POST /api/v1/sessions/{session_id}/continue
Content-Type: application/json

{
  "collected_data": {
    "review_priority": "high",
    "review_notes": "这是VIP客户的订单，请优先处理"
  }
}

1.3. 获取会话状态接口 (GET)
GET /api/v1/sessions/{session_id}
Authorization: Bearer <token>


1.4. 确认操作接口 (POST)
POST /api/v1/sessions/{session_id}/confirm
Content-Type: application/json

{
  "confirmed": true,
  "confirmation_data": {} // 可选的额外确认数据
}
1.4. 销毁操作接口 (POST)
POST /api/v1/sessions/{session_id}/terminate
Content-Type: application/json

 
//关键接口类— 
export interface Session {
  session_id: string;
  current_step: string;
  next_action: NextAction;
  progress: Progress;
  data?: any;
}

export interface NextAction {
  type: 'wait' | 'collect_info' | 'execute' | 'confirm' | 'terminate';
  instruction: Instruction;
}

export interface Instruction {
  title?: string;
  message?: string;  
  action?: string;
  target?: string;
  parameters?: any; 
}
 

export interface Progress {
  current: number;
  total: number;
  message: string;
}


Session的样例数据：

样例：
{
  "session_id": "sess_001",
  "current_step": "collecting_order_info",
  "next_action": {
    "type": "collect_info",
    "instruction": {
      "title": "订单审核信息收集", 
      "submit_url": "/api/v1/sessions/sess_001/continue"
    }
  },
  "progress": {
    "current": 1,
    "total": 4,
    "message": "正在准备订单审核流程..."
  }
}

### 2.前端： 类似DeepseekSeek的 对话框
2.1 显示任务总进展
2.2 流水输出

### 3. 后端 

##3.1 产品本体数据服务设计
###3.1.1 类设计
class ProductOntoService {  //接口定义
接口1：按销售目节点名称查找目录节点 querySalesCatalogNodes
  req: sales_catalog_id=“001”, node_name(模糊搜索）
  resp: List<CatalogNode> catalog_nodes;


接口3：按目录节点获取产品列表 queryProductByNode
  req: node_codes
  resp: List<Product> products; 

接口2: 解析规格 parseProductSpecs
  req: node_code, orignal_specs:List<String>
  rsp: ProductSpecficationReq

产品接口1： 获取指定产品的规格列表 queryProductSpecfication
  req: product_code
  rsq: ProductSpecfication

产品接口2： 获取指定产品的参数列表 queryProductParameter
  req: product_code
  rsq: ProductParameter

} 

 class CatalogNode{
  String fatherCode;
  String code;
  String name; 
}

class Product{
   String fatherCode;
   String code;
   long id;
   String name;
}


class Specfication{
	String specName;
	String compare;//有：“>、>=,=,<,<="操作符
    String specValue;//有几种类型，数字类型-如：“1“，  字符类型-如:"GE", 列表类型-如:"大,中，小"
    String unit;
     public NOT_FOUND= new Specfication("NOT_FOUND");
}

 
class ProductSpecfication{
	String productCode;
	List<Specfication> specs;	 
}

class ProductParameter {
	String productCode;
	List<Parameter> paras;
	
}

class Parameter{
   String code;
   String type;//类型，选择性，输入型
   String defaultValue;//建议值
   int sortNo;//配置
   List<String> options;//可选值列表
   String refSpecCode;//引用的规格Code
}

###3.1.1 样例数据  
  
按ProductOntoService接口，实现下列模块数据 ProductOntoService4Local

1、样例数据采用json存储，参考ModuleUtils改写一个ProductOntoUtils
2、数据如下
-企业市场
---服务器 CatalogNode
----数据中心服务器  CatalogNode spec11  spec12  spec13
------PowerEdge R760xa spec111  spec121  spec131  para11 para21 para31
------PowerEdge R860xa spec112  spec122  spec132  para12 para22 para32
------PowerEdge T860xa spec113  spec123  spec133  para13 para23 para33
----家庭服务器  CatalogNode  spec21  spec22  spec23
------Dell 16 Plus   Product  
------Dell 17 Plus   Product

---ONU
-----OptiXstar P813E-E
-----OptiXstar P813L
-----OptiXstar P813W


spec11:目录节点上
class Specfication{
	  specName = "最高工作环境温度”;
	  compare="="
      specValue=""
      unit="°C"
}

 
spec111:
class Specfication{
	  specName = "最高工作环境温度”;
	  compare="="
      specValue="-40"
      unit="°C"
	 
}





##3.2产品Agent服务的设计
 
 
###3.2.1核心业务对象
 
class SpecficationReq{
	String orignalSpec;
	List<Specfication> stdSpecs;//标准规格，满足其中一条就可以
}
class ProductSpecficationReq{//产品级的规格需求
	String catalogNode;//表示本目录下的产品
	List<SpecficationReq> specReqs;	 
}

class ConfigReq{ //配置需求
 String productSerial;//产品系列
 int totalQuantity;//总套数
 List<String> specReqItems;//规格需求项
 ConfigStrategy configStrategy;//配置策略    
 String totalQuantityMemo;//总套数
}

enum ConfigStrategy{
	PRICE_MIN_PRIORITY,//目录价最小优先，默认最新
    TECH_MAX_PRIORITY, //技术最大优先对
}
  

//参数配置结果
class ParameterConfig{
  String code;//参数code
  String value;//配置的结果值
  String inference;//推理过程
  boolean needCheck=false;
  String checkPoint;//确认点 
}
class ProductConfigIntent {
  String productCode;
  List<ParameterConfigIntent> paras;
	
}
class ParameterConfigIntent
{
	String code;//参数code	
	List<ParameterConfigIntentOption>  intentOptions;
	ParameterConfig result;//配置结果
	Parameter base;//基础数据
}
class ParameterConfigIntentOption {
	String value;//意图的值
	String message;//意图的信息
	booolean isVisited= false;//
}

class ProductConfig {
  String productCode;
  List<ParameterConfig> paras;
  CheckResult checkResult;
}
 
 class Plan {
	public String STEP1 = "step1";
	public String STEP2 = "step2";
	public String STEP3 = "step3";
	List<String> tasks = Arrays.asList(STEP1,STEP2,STEP3);
}


###3.2.2 关键服务
class  PCAgentConstroller {
    PCAgentService agentService;
    .....
    参考前后台接口
        调用agentService.doGenneratorConfig()//首次提交
        后续通过agentService.getSession获取过程信息

}
class  PCAgentService {
	Map<sessionId,PCAgentService4Session> sessions;
     PCAgentService4Session doGenneratorConfig(String sessionId,String userInput){
        每个userInput产生一个新的PCAgentService4Session
     } 
     Session getLatestSession(String sessionId) {
        PCAgentService4Session sessionSerivice = sessions.get(sessionId)；
        return sessionSerivice.currentSession;
     }
}

class  PCAgentService4Session {
		
	ProductOntoService  productOntoService;
	ProductSpecficationParserService specParserService;
	ProductSelectionService selectionSerivice;
	ProductConfigService  configService;
    String sessionId； 
	Session currentSession;
    void doGenneratorConfig(String sessionId,String userInput){
		session  = createSession(sessionId);
		//调用parseConfigReq解析客户需求
		req = parseConfigReq(  input);
		updateSession4NextStep(req,Plan.STEP1);
		
		//解析规格
		List<ProductSpecReq> productSpecReqByCatalogNode = specParserService.parseProductSpecs(req.productSerial,req.specReqItems)
		updateSession4NextStep(productSpecReqByCatalogNode,Plan.STEP2);

		//和产品选型  
		Pair<List<ProductDeviationDegree>,ProductDeviationDegree> selectionResult = selectionSerivice.selectProduct(productSpecReqByCatalogNode);
		updateSession4NextStep(selectionResult,Plan.STEP2);

		//参数配置结果
		ProductConfig config= configService.doParameterConfigs(selectionResult.second, req) ;
		updateSession4NextStep(config,Plan.STEP3);
		//进行总结
	}
   
	 

	ConfigReq parseConfigReq(String input){
		//调用LLMInvoker，config_req_parse_prompt.jtl  TODO 
		//调用validConfigReq进行校验   
	}
	void volidConfigReq(ConfigReq req){ //不合理抛异常 InvalidInputException(RuntimeException)
		//....
	
	}
	 
}



//产品配置服务-参数
class ProductConfigService{
	ProductOntoService  productOntoService;
	
	public ProductConfig doParameterConfigs(ProductDeviationDegree productDeviationDegree,ConfigReq req) {
		ProductParameter productPara = productOntoService.queryProductParameter(productDeviationDegree.productCode);
		productPara.getParas() 把参数按sortNo排序（从小到大） -->paras;
		//生成配置意图
		List<ParameterConfigIntent> paraIntents = genneratParameterConfigIntents(paras,productDeviationDegree,totalQuantity); 	
		
		Stack<ParameterConfigIntent> paraConfigResult = autoParaConfig(paraIntents);

		根据paraConfigResult生成ProductConfig；
	}

	//根据配置意图自动生成
	Stack<ParameterConfigIntent> autoParaConfig(List<ParameterConfigIntent> paraIntents){

		//进行自动配置（算法） 
		Stack<ParameterConfigIntent>  paraConfigResultStack =...
		//首次解，按第一个进行遍历
		for(paraIntent: paraIntents){ 
			paraConfigResultStack.push(paraIntent);
			boolean hasSolution = false;//是否有解
			for(intentOption: paraIntent.intentOptions) {
				if(intentOption.isVisited == false) {	
					paraIntent.result.value = intentOption.value;
					intentOption.isVisited  = false;//TODO：后续要推理逻辑在里面
					hasSolution = true;
					break;
				}
			} 
			if(hasSolution == false) {
				没有解的，warnning
			}
		}
		CheckResult checkResult = checkParameterConfig(paraConfigResultStack);
		if(checkResult.level == SUCCESS) {
			return paraConfigResultStack;
		}
		//paraConfigResultStack，对每个进行回溯
		....//调用这个进行访问 

	}


	CheckResult checkParameterConfig(Stack<ParameterConfigIntent>  paraConfigResultStack) {
		//调用LLM检查结果是否正确？
	}
	
	class CheckResult {
		CheckResultLevel level;//error
		int errorCode;//没有错误是0
		String errorMessage;
	}
	enum CheckResultLevel {
		ERROR(1),WARNING(2),SUCCESS(0);
	}

 
	//生成配置意图
	List<ParameterConfigIntent> genneratParameterConfigIntents(List<Parameter> parameters,
		ProductDeviationDegree productDeviationDegree,int totalQuantity) {
		 		//生成配置意图
		List<ParameterConfigIntent> paraIntents =....;
		for(parameter: productPara.getParamters()) {
			specItemDeviation = productDeviationDegree.querySpecItemDeviationDegree(parameter.refSpecCode);
			
			if(specItemDeviation == null ) {
				if(parameter.code.contain("QTY")
				{
					paraIntents.add(genneratParameterConfigIntent4Qty(parameter,req.totalQuantity);
				}else  {
				///log
			 }
			 paraIntents.add(genneratParameterConfigIntent(parameter,specItemDeviation);
		}
		return paraIntents;
	}


	ParameterConfigIntent genneratParameterConfigIntent4Qty(Parameter parameter, int reqQty) {
		.....
	}

	//生成配置意图
	ParameterConfigIntent genneratParameterConfigIntent(Parameter parameter,SpecItemDeviationDegree specItemDeviationDegree) {
		
		ParameterConfigIntent paraConfigIntent = new ParameterConfigIntent();
		paraConfigIntent.code = parameter.code; 
		paraConfigIntent.result = new ParameterConfig();//空
		//根据parameter.options 和 specItemDeviationDegree.stdSpecReq 来生成paraConfigIntent.options;
		....
	}
	
}
//产品选型服务
class ProductSelectionService{
	ProductOntoService  productOntoService;

	//做产品选型 
	public  Pair<List<ProductDeviationDegree>,ProductDeviationDegree> selectProduct(List<ProductSpecficationReq> productSpecReqGroup) {
			List<ProductDeviationDegree>  productDeviationDegrees = calcProductDeviationDegrees(productSpecReqGroup);
			result按deviationDegree降序排列 

			return new Pair<>(productDeviationDegrees,productDeviationDegrees.get(0));//第一个
		
	}
	 List<ProductDeviationDegree> calcProductDeviationDegrees(List<ProductSpecficationReq> productSpecReqGroup) {
		List<ProductDeviationDegree> result = ...;
		for(productSpecReqGroupItem: productSpecReqGroup) {
			products = productOntoService.queryProductByNode(productSpecReqGroupItem.catalogNode);
			for(product: products) {
				productSpec = productOntoService.queryProductSpecfication(product.code);
				productDeviationDegree = calcProductDeviationDegree(productSpec,productSpecReqGroupItem); 
				result.add(productDeviationDegree);
			}
		} 
		return result;		
	}
	//计算产品的偏离度
	ProductDeviationDegree calcProductDeviationDegree(ProductSpecfication productSpec, ProductSpecficationReq productSpecReq) { 
		result = new ProductDeviationDegree();
		for(specItemReq: productSpecReq.specItemReqs) {
			if(specItemReq ==  Specfication.NOT_FOUND) result.add(SpecItemDeviationDegree.buildNotFound(...))  
			result.addSpecItemDeviationDegree(calcSpecItemDeviationDegree(productSpec,specItemReq));
		}

		//计算result.totalDeviationDegrees
		return result
	}
	SpecItemDeviationDegree calcSpecItemDeviationDegree(ProductSpecfication productSpec, SpecficationReq specItemReq) {
		 
		 for(stdSpecItemReq specItemReq.stdSpecs){//匹配一条即可（后续考虑多条TODO）
			productSpecItem = productSpec.querySpecfication(stdSpecItemReq.name);
			if(productSpecItem == null) {
				//记录日志
			}
			return calcSpecItemDeviationDegree(productSpecItem,stdSpecItemReq);
		 }
		  //抛异常ParseProductSpecException
	}

	SpecItemDeviationDegree calcSpecItemDeviationDegree(Specfication specItem,Specfication stdSpecItemReq) {
		如果 stdSpecItemReq(specName1>=2), specItem(specName1==3), 则result = DeviationDegree.POSITIVE
		如果 stdSpecItemReq(specName1>=2), specItem(specName1==1), 则result = DeviationDegree.NEGATIVE
		如果 stdSpecItemReq(specName1>=2), specItem(specName1==2), 则result = DeviationDegree.NONE
		如果 stdSpecItemReq = Specfication.NOT_FOUND 则result = DeviationDegree.NOT_FOUND 
	}

	class  ProductDeviationDegree{
		String productCode;
		List<SpecItemDeviationDegree> specItemDeviationDegrees;
		int totalDeviationDegrees;//总体满足度，如80%
		@jsonignore
		Map<String,SpecItemDeviationDegree> specItemDeviationDegreesMap;
		SpecItemDeviationDegree querySpecItemDeviationDegree(String specName){...};
	}
	class  SpecItemDeviationDegree{
		String orignalSpecReq;//原始的规格需求描述
		String spectName;//标准的规格名称
		booolean satisfy=false;//是否满足
		DeviationDegree deviationDegree;//偏离度
		Specfication stdSpecReq；//标准规格的的信息1
		
		SpecItemDeviationDegree buildNotFound(orignalSpec,spectName){....}
	}
	enum DeviationDegree{
		NOT_FOUND("not found"),
		POSITIVE("positive deviation"),
		NEGATIVE("negative deviation"),
		NONE("no deviation"), 
	}
 
}


class ProductSpecficationParserService{
		ProductOntoService  productOntoService;
	List<Specfication> parseProductSpecs(List<String> specReqItems){
		调用ProductOntoService.parseProductSpecs
		错误抛异常 ParseProductSpecException(RuntimeException)
	}

	//规格解析
	ProductSpecReq parseProductSpecs(String catalogNode，List<String> specReqItems){//错误抛异常 ParseProductSpecException(RuntimeException) 
		ProductSpecReq productSpecReq = new ....;//catalogNode 
		for(specReqItem: specReqItems) {
			specs = ProductOntoService.parseProductSpecs(catalogNode.node_code)
			productSpecReq.addSpecReqs(catalogNode,specs);
		}
		return productSpecReq;		
	}

	//规格解析 
	public List<ProductSpecReq> parseProductSpecs(String productSerial，List<String> specReqItems){//错误抛异常 ParseProductSpecException(RuntimeException) 
		catalogNodes = ProductOntoService.querySalesCatalogNodes(node_name=productSerial) 
		for(catalogNode: catalogNodes) {
			specs = ProductOntoService.parseProductSpecs(catalogNode.node_code,specReqItems)
			//如果找不到，则返回Specfication.NOT_FOUND
			result.add(specs);
		}
		return result;		
	}
} 

class SessionUitils {
	String nextSessionId() {
		return UUID;
	}
	Session create(sessionId,Plan plan) {
		Session session=  new Session();
		session.session_id =  ;
		session.current_step = "";
		session.current_step = "execute";
		session.progress.current= 0;
		session.progress.total= plan.tasks.length;
		session.progress.total= plan.tasks.length;
		session.progress.message= ""; 
		return session;
	}
	Session updateSession4NextStep(Session currentSession, Object data,String c)
	{
		//根据data，next_step,更新seesion信息
	}
	Session updateSession4CurrentStep(Session currentSession, Object data)
	{
		//根据data， 更新seesion信息（仅更新data）
	} 
	//TODO ；后续支持Session的存储，暂时不实现
}



 


 

 
