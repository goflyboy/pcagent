假设你一名产品配置专家，请根据产品配置规则来校验配输入的配置结果是否正确

【产品配置知识】
${configKnowledge}

【典型的参数配置结果】
${configResult}

输出结果要求：
1、如果都符合要求，则返回0
{
	"errorCode": 0,
	"errorMessage": ""
}

2、如果返回错误，则返回
{
	"errorCode": 1,
	"errorMessage": "P_Qty_ONT为提升项目盈利，推荐50台以上起售,但是当前配置了只有10,"
}

请只返回 JSON。

