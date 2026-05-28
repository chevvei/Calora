package com.calora.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FoodClassification(
    val foodName: String,
    val confidence: Float
)

data class FoodResult(
    val foodName: String,
    val confidence: Float,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

class FoodClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.3f)
                .build()
        )
    }

    private val englishToChinese = mapOf(
        "Pizza" to "披萨",
        "Hamburger" to "汉堡",
        "Hot dog" to "热狗",
        "Hotdog" to "热狗",
        "French fries" to "薯条",
        "French loaf" to "法棍面包",
        "Bagel" to "贝果",
        "Pretzel" to "椒盐脆饼",
        "Bread" to "面包",
        "Guacamole" to "牛油果酱",
        "Burrito" to "墨西哥卷饼",
        "Taco" to "塔可",
        "Nachos" to "玉米片",
        "Steak" to "牛排",
        "Meat loaf" to "肉饼",
        "Filet mignon" to "菲力牛排",
        "Prime rib" to "上等肋排",
        "Ribeye" to "肋眼牛排",
        "Pork chop" to "猪排",
        "Bacon" to "培根",
        "Ham" to "火腿",
        "Sausage" to "香肠",
        "Chicken" to "鸡肉",
        "Fried chicken" to "炸鸡",
        "Roast chicken" to "烤鸡",
        "Turkey" to "火鸡",
        "Duck" to "鸭肉",
        "Fish" to "鱼",
        "Salmon" to "三文鱼",
        "Sushi" to "寿司",
        "Sashimi" to "刺身",
        "Lobster" to "龙虾",
        "Crab" to "螃蟹",
        "Shrimp" to "虾",
        "Oyster" to "生蚝",
        "Clam" to "蛤蜊",
        "Mussel" to "青口贝",
        "Soup" to "汤",
        "Potpie" to "馅饼",
        "Consomme" to "清汤",
        "Bouillon" to "肉汤",
        "Salad" to "沙拉",
        "Caesar salad" to "凯撒沙拉",
        "Coleslaw" to "凉拌卷心菜",
        "Ice cream" to "冰淇淋",
        "Ice lolly" to "冰棒",
        "Popsicle" to "冰棒",
        "Cake" to "蛋糕",
        "Chocolate cake" to "巧克力蛋糕",
        "Strawberry" to "草莓",
        "Apple" to "苹果",
        "Orange" to "橙子",
        "Banana" to "香蕉",
        "Pineapple" to "菠萝",
        "Lemon" to "柠檬",
        "Fig" to "无花果",
        "Pomegranate" to "石榴",
        "Grapes" to "葡萄",
        "Mango" to "芒果",
        "Watermelon" to "西瓜",
        "Cantaloupe" to "哈密瓜",
        "Coconut" to "椰子",
        "Pear" to "梨",
        "Peach" to "桃子",
        "Egg" to "鸡蛋",
        "Eggs" to "鸡蛋",
        "Cheese" to "奶酪",
        "Milk" to "牛奶",
        "Coffee" to "咖啡",
        "Tea" to "茶",
        "Espresso" to "浓缩咖啡",
        "Latte" to "拿铁",
        "Cappuccino" to "卡布奇诺",
        "Beer" to "啤酒",
        "Wine" to "红酒",
        "Cocktail" to "鸡尾酒",
        "Juice" to "果汁",
        "Rice" to "白米饭",
        "Noodle" to "面条",
        "Noodles" to "面条",
        "Spaghetti" to "意面",
        "Pasta" to "意面",
        "Macaroni" to "通心粉",
        "Dumpling" to "饺子",
        "Dumplings" to "饺子",
        "Wonton" to "馄饨",
        "Tofu" to "豆腐",
        "Mushroom" to "蘑菇",
        "Corn" to "玉米",
        "Potato" to "土豆",
        "Tomato" to "番茄",
        "Carrot" to "胡萝卜",
        "Broccoli" to "西兰花",
        "Cucumber" to "黄瓜",
        "Cabbage" to "卷心菜",
        "Onion" to "洋葱",
        "Garlic" to "大蒜",
        "Pepper" to "辣椒",
        "Sandwich" to "三明治",
        "Club sandwich" to "总会三明治",
        "Croissant" to "牛角面包",
        "Muffin" to "玛芬",
        "Pancake" to "煎饼",
        "Waffle" to "华夫饼",
        "Dough" to "面团",
        "Cookie" to "曲奇",
        "Brownie" to "布朗尼",
        "Candy" to "糖果",
        "Chocolate" to "巧克力",
        "Honey" to "蜂蜜",
        "Jam" to "果酱",
        "Peanut" to "花生",
        "Almond" to "杏仁",
        "Walnut" to "核桃",
        "Cashew" to "腰果",
        "Porridge" to "粥",
        "Oatmeal" to "燕麦粥",
        "Cereal" to "麦片",
        "Yogurt" to "酸奶",
        "Soy milk" to "豆浆",
        "Dim sum" to "点心",
        "Spring roll" to "春卷",
        "Egg roll" to "蛋卷",
        "Fried rice" to "炒饭",
        "Fried noodle" to "炒面",
        "Kung pao chicken" to "宫保鸡丁",
        "Mapo tofu" to "麻婆豆腐",
        "Sweet and sour" to "糖醋",
        "Teriyaki" to "照烧",
        "Tempura" to "天妇罗",
        "Ramen" to "拉面",
        "Udon" to "乌冬面",
        "Miso soup" to "味噌汤",
        "Kimchi" to "泡菜",
        "Curry" to "咖喱",
        "Naan" to "馕",
        "Dosa" to "薄饼",
        "Paella" to "西班牙海鲜饭",
        "Risotto" to "意大利烩饭",
        "Bruschetta" to "意式烤面包",
        "Lasagna" to "千层面",
        "Ravioli" to "意大利饺",
        "Quiche" to "法式咸派",
        "Crepe" to "可丽饼",
        "Fondue" to "火锅",
        "Hot pot" to "火锅",
        "Lychee" to "荔枝",
        "Litchi" to "荔枝",
        "Longan" to "龙眼",
        "Persimmon" to "柿子",
        "Kiwifruit" to "猕猴桃",
        "Kiwifruit" to "猕猴桃",
        "Kiwi" to "猕猴桃",
        "Dragon fruit" to "火龙果",
        "Pitaya" to "火龙果",
        "Durian" to "榴莲",
        "Jackfruit" to "菠萝蜜",
        "Mangosteen" to "山竹",
        "Passion fruit" to "百香果",
        "Star fruit" to "杨桃",
        "Loquat" to "枇杷",
        "Jujube" to "红枣",
        "Red date" to "红枣",
        "Chinese date" to "红枣",
        "Mulberry" to "桑葚",
        "Plum" to "李子",
        "Cherry" to "樱桃",
        "Apricot" to "杏",
        "Avocado" to "牛油果",
        "Pomelo" to "柚子",
        "Grapefruit" to "西柚",
        "Rambutan" to "红毛丹",
        "Sugarcane" to "甘蔗",
        "Tangerine" to "橘子",
        "Clementine" to "小橘子",
        "Mandarin" to "柑橘",
        "Plantain" to "大蕉",
        "Papaya" to "木瓜",
        "Guava" to "番石榴",
        "Date" to "椰枣",
        "Rhubarb" to "大黄",
        "Artichoke" to "朝鲜蓟",
        "Asparagus" to "芦笋",
        "Eggplant" to "茄子",
        "Zucchini" to "西葫芦",
        "Pumpkin" to "南瓜",
        "Squash" to "南瓜",
        "Radish" to "萝卜",
        "Turnip" to "芜菁",
        "Beet" to "甜菜",
        "Spinach" to "菠菜",
        "Celery" to "芹菜",
        "Lettuce" to "生菜",
        "Kale" to "羽衣甘蓝",
        "Bok choy" to "小白菜",
        "Edamame" to "毛豆",
        "Green bean" to "四季豆",
        "Soybean" to "大豆",
        "Lentil" to "扁豆",
        "Chickpea" to "鹰嘴豆",
        "Tofu" to "豆腐",
        "Soy milk" to "豆浆",
        "Green tea" to "绿茶",
        "Black tea" to "红茶",
        "Milk tea" to "奶茶",
        "Matcha" to "抹茶",
        "Mocha" to "摩卡",
        "Smoothie" to "冰沙",
        "Milkshake" to "奶昔",
        "Soda" to "汽水",
        "Cola" to "可乐",
        "Lemonade" to "柠檬水",
        "Cider" to "苹果酒",
        "Sake" to "清酒",
        "Soju" to "烧酒",
        "Whiskey" to "威士忌",
        "Vodka" to "伏特加",
        "Rum" to "朗姆酒",
        "Gin" to "金酒",
        "Brandy" to "白兰地",
        "Champagne" to "香槟",
        "Bento" to "便当",
        "Congee" to "粥",
        "Porridge" to "粥",
        "Rice ball" to "饭团",
        "Onigiri" to "饭团",
        "Rice cake" to "年糕",
        "Mochi" to "麻薯",
        "Red bean" to "红豆",
        "Mung bean" to "绿豆",
        "Lotus root" to "莲藕",
        "Bamboo shoot" to "竹笋",
        "Water chestnut" to "马蹄",
        "Taro" to "芋头",
        "Sweet potato" to "红薯",
        "Yam" to "山药",
        "Cassava" to "木薯",
        "Seaweed" to "海苔",
        "Nori" to "海苔",
        "Kelp" to "海带",
        "Dried tofu" to "豆干",
        "Century egg" to "皮蛋",
        "Pickled vegetable" to "咸菜",
        "Fermented bean curd" to "腐乳",
        "Soy sauce" to "酱油",
        "Vinegar" to "醋",
        "Sesame oil" to "香油",
        "Chili oil" to "辣椒油",
        "Sriracha" to "是拉差酱",
        "Ketchup" to "番茄酱",
        "Mustard" to "芥末",
        "Mayonnaise" to "蛋黄酱",
        "Butter" to "黄油",
        "Cream" to "奶油",
        "Whipped cream" to "鲜奶油",
        "Condensed milk" to "炼乳",
        "Evaporated milk" to "淡奶",
        "Coconut milk" to "椰奶",
        "Oat milk" to "燕麦奶",
        "Almond milk" to "杏仁奶"
    )

    private val foodKeywords = setOf(
        "food", "dish", "meal", "cuisine", "recipe", "plate",
        "pizza", "burger", "sandwich", "salad", "soup", "steak",
        "chicken", "fish", "rice", "noodle", "pasta", "bread",
        "cake", "ice cream", "fruit", "vegetable", "dessert",
        "sushi", "taco", "burrito", "curry", "dumpling",
        "dim sum", "hot pot", "fried", "roast", "grill",
        "breakfast", "lunch", "dinner", "snack", "appetizer",
        "seafood", "shellfish", "mollusk", "crustacean",
        "produce", "berry", "melon", "citrus", "tropical",
        "legume", "grain", "dairy", "meat", "poultry",
        "beverage", "drink", "condiment", "sauce", "spice",
        "snack", "pastry", "confection"
    )

    suspend fun classify(imageUri: Uri): FoodResult {
        val bitmap = loadBitmap(imageUri)
        val labels = classifyWithMlKit(bitmap)
        val foodLabel = findBestFoodLabel(labels)
        val chineseName = englishToChinese[foodLabel.text] ?: foodLabel.text
        val nutrition = com.calora.nutrition.NutritionEstimator().estimate(chineseName)
        return FoodResult(
            foodName = chineseName,
            confidence = foodLabel.confidence,
            calories = nutrition.calories,
            protein = nutrition.protein,
            carbs = nutrition.carbs,
            fat = nutrition.fat
        )
    }

    private suspend fun classifyWithMlKit(bitmap: Bitmap): List<ImageLabel> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    continuation.resume(labels)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    private fun findBestFoodLabel(labels: List<ImageLabel>): ImageLabel {
        for (label in labels) {
            if (englishToChinese.containsKey(label.text)) {
                return label
            }
        }
        for (label in labels) {
            val lower = label.text.lowercase()
            for (keyword in foodKeywords) {
                if (lower.contains(keyword)) {
                    return label
                }
            }
        }
        return labels.firstOrNull() ?: ImageLabel("未知食物", 0f, 0)
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }
}
