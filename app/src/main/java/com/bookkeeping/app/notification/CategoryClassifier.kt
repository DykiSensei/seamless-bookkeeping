package com.bookkeeping.app.notification

import com.bookkeeping.app.data.local.entity.TransactionCategory

// 自动分类器：根据商户名 + 通知/短信文本，推测 TransactionCategory。
// 命中第一个关键词即返回；都不命中返回 OTHER。
//
// 嵌入式类比：一个查找表（lookup table），关键词进，分类出。
// 后续可演进为：用户手动改分类后记住"商户→分类"映射，下次自动用。
object CategoryClassifier {

    // 关键词分组。顺序敏感：更明确的规则放前面（如"工资到账"应该早于通用的"到账"）
    private val RULES: List<Pair<List<String>, TransactionCategory>> = listOf(
        // 工资 / 收入相关（优先匹配，避免被其他分类抢走）
        listOf("工资", "薪资", "薪水", "代发", "奖金", "津贴", "年终") to TransactionCategory.SALARY,

        // 餐饮（咖啡 / 茶饮 / 快餐 / 中餐 / 外卖）
        listOf(
            "星巴克", "瑞幸", "Costa", "Tims", "Peet",
            "茶颜悦色", "喜茶", "奈雪", "蜜雪冰城", "霸王茶姬", "古茗", "茶百道", "沪上阿姨", "CoCo",
            "麦当劳", "肯德基", "KFC", "汉堡王", "Burger", "必胜客", "赛百味", "塔斯汀",
            "海底捞", "西贝", "外婆家", "呷哺", "南城香", "和府捞面",
            "美团", "饿了么", "盒马", "叮咚",
            "餐厅", "酒楼", "饭店", "茶饮", "烧烤", "火锅", "面馆", "餐饮",
        ) to TransactionCategory.FOOD,

        // 交通
        listOf(
            "滴滴", "高德打车", "T3", "曹操", "首汽", "出租车", "网约车",
            "12306", "高铁", "动车", "火车", "铁路",
            "航空", "机票", "民航", "南航", "国航", "东航", "海航",
            "地铁", "公交", "公共交通",
            "ETC", "停车", "加油", "中石化", "中石油", "充电站",
            "共享单车", "哈啰", "美团单车", "青桔",
        ) to TransactionCategory.TRANSPORT,

        // 购物（电商 / 便利店 / 商超 / 数码）
        listOf(
            "淘宝", "天猫", "京东", "拼多多", "唯品会", "1688", "考拉",
            "苹果", "Apple", "华为", "小米", "OPPO", "vivo", "荣耀",
            "Nike", "Adidas", "优衣库", "Uniqlo", "无印良品", "MUJI", "宜家", "IKEA", "网易严选",
            "7-11", "7-Eleven", "全家", "FamilyMart", "罗森", "Lawson", "便利蜂", "美宜佳", "便利店",
            "永辉", "沃尔玛", "Walmart", "Costco", "山姆", "家乐福", "超市",
        ) to TransactionCategory.SHOPPING,

        // 娱乐 / 数字订阅
        listOf(
            "Steam", "Epic", "PlayStation", "PSN", "Switch", "任天堂", "Xbox",
            "腾讯视频", "爱奇艺", "优酷", "Netflix", "Disney", "HBO", "Spotify", "Apple Music", "网易云",
            "电影院", "万达", "CGV", "UME", "大地影院",
            "B站", "bilibili", "抖音", "快手",
            "KTV", "唱吧", "游戏", "氪金", "充值",
        ) to TransactionCategory.ENTERTAINMENT,

        // 住房
        listOf("房租", "租金", "物业", "中介费", "押金", "二房东") to TransactionCategory.HOUSING,

        // 水电 / 公用事业
        listOf("水费", "电费", "燃气费", "燃气", "热力", "网费", "宽带", "话费", "流量充值") to TransactionCategory.UTILITIES,

        // 医疗
        listOf("医院", "药店", "药房", "挂号", "诊所", "体检", "口腔", "牙科", "眼科") to TransactionCategory.HEALTH,

        // 教育
        listOf("学费", "培训", "课程", "网课", "得到", "极客时间", "教材", "书店", "当当", "Kindle", "Audible") to TransactionCategory.EDUCATION,

        // 转账
        listOf("转账", "转入", "转出", "汇款") to TransactionCategory.TRANSFER,
    )

    // 输入：商户名 + 上下文文本（如通知正文、短信正文）
    // 输出：匹配到的分类，没有就 OTHER
    fun classify(merchant: String, contextText: String = ""): TransactionCategory {
        val haystack = "$merchant $contextText"
        for ((keywords, category) in RULES) {
            for (keyword in keywords) {
                if (haystack.contains(keyword, ignoreCase = true)) {
                    return category
                }
            }
        }
        return TransactionCategory.OTHER
    }
}
