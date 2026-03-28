package com.example.waredobe2

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileOutputStream
import java.util.*

/* ---------------- DATA ---------------- */

data class WardrobeItem(
    val id: String,
    val name: String,
    val season: String,
    val colorValue: Long, // Сохраняем значение цвета как Long
    val photoFileName: String?
) {
    val color: Color get() = Color(colorValue)
}

data class Outfit(
    val id: String,
    val name: String,
    val itemIds: List<String>
)

/* ---------------- VIEWMODELS ---------------- */

class WardrobeViewModel : ViewModel() {
    val items = mutableStateListOf<WardrobeItem>()

    fun addItem(item: WardrobeItem, context: Context) {
        items.add(item)
        saveItemsToStorage(context)
    }

    fun removeItem(itemId: String, context: Context) {
        val item = items.find { it.id == itemId }
        item?.let {
            // Удаляем файл изображения, если он существует
            if (it.photoFileName != null) {
                val file = File(context.filesDir, it.photoFileName)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        items.removeAll { it.id == itemId }
        saveItemsToStorage(context)
    }

    fun saveItemsToStorage(context: Context) {
        val prefs = context.getSharedPreferences("wardrobe", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("items_count", items.size)

        items.forEachIndexed { index, item ->
            editor.putString("item_${index}_id", item.id)
            editor.putString("item_${index}_name", item.name)
            editor.putString("item_${index}_season", item.season)
            editor.putLong("item_${index}_color", item.colorValue)
            editor.putString("item_${index}_photo", item.photoFileName)
        }
        editor.apply()
    }

    fun loadItemsFromStorage(context: Context) {
        val prefs = context.getSharedPreferences("wardrobe", Context.MODE_PRIVATE)
        val count = prefs.getInt("items_count", 0)

        val loadedItems = mutableListOf<WardrobeItem>()
        for (i in 0 until count) {
            val id = prefs.getString("item_${i}_id", "") ?: ""
            val name = prefs.getString("item_${i}_name", "") ?: ""
            val season = prefs.getString("item_${i}_season", "Демисезон") ?: "Демисезон"
            val colorValue = prefs.getLong("item_${i}_color", PrimaryColor.toLongValue())
            val photoFileName = prefs.getString("item_${i}_photo", null)

            if (id.isNotBlank() && name.isNotBlank()) {
                loadedItems.add(WardrobeItem(id, name, season, colorValue, photoFileName))
            }
        }

        items.clear()
        items.addAll(loadedItems)
    }
}

class OutfitsViewModel : ViewModel() {
    val outfits = mutableStateListOf<Outfit>()

    fun addOutfit(outfit: Outfit, context: Context) {
        outfits.add(outfit)
        saveOutfitsToStorage(context)
    }

    fun removeOutfit(outfitId: String, context: Context) {
        outfits.removeAll { it.id == outfitId }
        saveOutfitsToStorage(context)
    }

    fun saveOutfitsToStorage(context: Context) {
        val prefs = context.getSharedPreferences("outfits", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("outfits_count", outfits.size)

        outfits.forEachIndexed { index, outfit ->
            editor.putString("outfit_${index}_id", outfit.id)
            editor.putString("outfit_${index}_name", outfit.name)
            editor.putString("outfit_${index}_items", outfit.itemIds.joinToString(","))
        }
        editor.apply()
    }

    fun loadOutfitsFromStorage(context: Context) {
        val prefs = context.getSharedPreferences("outfits", Context.MODE_PRIVATE)
        val count = prefs.getInt("outfits_count", 0)

        val loadedOutfits = mutableListOf<Outfit>()
        for (i in 0 until count) {
            val id = prefs.getString("outfit_${i}_id", "") ?: ""
            val name = prefs.getString("outfit_${i}_name", "") ?: ""
            val itemsStr = prefs.getString("outfit_${i}_items", "") ?: ""
            val itemIds = if (itemsStr.isNotBlank()) itemsStr.split(",") else emptyList()

            if (id.isNotBlank() && name.isNotBlank()) {
                loadedOutfits.add(Outfit(id, name, itemIds))
            }
        }

        outfits.clear()
        outfits.addAll(loadedOutfits)
    }
}

/* ---------------- NAVIGATION ---------------- */

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object List : Screen("list", "Гардероб", Icons.Default.List)
    object Add : Screen("add", "Добавить", Icons.Default.Add)
    object Outfits : Screen("outfits", "Образы", Icons.Default.Favorite)
}

/* ---------------- COLORS & THEME ---------------- */

private val PrimaryColor = Color(0xFF6C5CE7)
private val SecondaryColor = Color(0xFFFF7675)
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFEAA7), Color(0xFF81ECEC))
)
private val SurfaceColor = Color.White.copy(alpha = 0.85f)
private val OnPrimaryColor = Color.White

// Функция для преобразования Color в Long
// Измените функцию преобразования цвета в Long
private fun Color.toLongValue(): Long {
    // Преобразуем Color в ARGB Long
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    val alpha = (this.alpha * 255).toInt()
    return ((alpha shl 24) or (red shl 16) or (green shl 8) or blue).toLong()
}

/* ---------------- ROOT ---------------- */

@Composable
fun WardrobeApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val wardrobeVm = remember { WardrobeViewModel() }
    val outfitsVm = remember { OutfitsViewModel() }

    // Загружаем данные при первом запуске
    LaunchedEffect(Unit) {
        wardrobeVm.loadItemsFromStorage(context)
        outfitsVm.loadOutfitsFromStorage(context)
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryColor,
            secondary = SecondaryColor,
            background = Color.Transparent,
            surface = SurfaceColor,
            onPrimary = OnPrimaryColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = { BottomBar(nav) }
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Screen.List.route,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Screen.List.route) {
                        WardrobeList(
                            wardrobeVm = wardrobeVm,
                            outfitsVm = outfitsVm,
                            context = context
                        )
                    }
                    composable(Screen.Add.route) {
                        AddItemScreen(
                            wardrobeVm = wardrobeVm,
                            context = context,
                            onSaved = { nav.navigate(Screen.List.route) }
                        )
                    }
                    composable(Screen.Outfits.route) {
                        OutfitsScreen(
                            wardrobeVm = wardrobeVm,
                            outfitsVm = outfitsVm,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- BOTTOM BAR ---------------- */

@Composable
fun BottomBar(nav: NavHostController) {
    val screens = listOf(Screen.List, Screen.Add, Screen.Outfits)
    NavigationBar(
        containerColor = PrimaryColor.copy(alpha = 0.9f),
        tonalElevation = 10.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .fillMaxWidth()
    ) {
        val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
        screens.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = { nav.navigate(screen.route) },
                icon = { Icon(screen.icon, null) },
                label = { Text(screen.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SecondaryColor,
                    unselectedIconColor = OnPrimaryColor.copy(alpha = 0.7f),
                    selectedTextColor = SecondaryColor,
                    unselectedTextColor = OnPrimaryColor.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/* ---------------- HELPERS: ItemPhotoCard & FullscreenImage ---------------- */

@Composable
fun ItemPhotoCard(
    item: WardrobeItem,
    context: Context,
    modifier: Modifier = Modifier,
    onClick: (Uri?) -> Unit
) {
    Card(
        modifier = modifier
            .size(90.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clickable {
                val uri = if (item.photoFileName != null) {
                    val file = File(context.filesDir, item.photoFileName)
                    Uri.fromFile(file)
                } else {
                    null
                }
                onClick(uri)
            },
        shape = RoundedCornerShape(8.dp)
    ) {
        if (item.photoFileName != null) {
            val file = File(context.filesDir, item.photoFileName)
            if (file.exists()) {
                Image(
                    painter = rememberAsyncImagePainter(Uri.fromFile(file)),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(item.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет фото", color = Color.White)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(item.color),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет фото", color = Color.White)
            }
        }
    }
}

@Composable
fun FullscreenImage(photo: Uri?, onClose: () -> Unit) {
    if (photo == null) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(photo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentScale = ContentScale.Fit
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.rotate(45f)
            )
        }
    }
}

/* ---------------- WARDROBE LIST ---------------- */

@Composable
fun WardrobeList(
    wardrobeVm: WardrobeViewModel,
    outfitsVm: OutfitsViewModel,
    context: Context
) {
    var photoToView by remember { mutableStateOf<Uri?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wardrobeVm.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Добавь первую вещь", color = PrimaryColor, fontSize = 18.sp)
            }
        } else {
            LazyColumn(Modifier.padding(16.dp)) {
                items(wardrobeVm.items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ItemPhotoCard(
                                item = item,
                                context = context,
                                modifier = Modifier,
                                onClick = { uri -> photoToView = uri }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 18.sp, color = PrimaryColor)
                                Text(item.season, color = SecondaryColor)
                            }
                            IconButton(
                                onClick = {
                                    // Проверяем, используется ли вещь в образах
                                    val usedInOutfits = outfitsVm.outfits.any { outfit ->
                                        outfit.itemIds.contains(item.id)
                                    }

                                    if (usedInOutfits) {
                                        // Если используется, удаляем из всех образов
                                        val updatedOutfits = outfitsVm.outfits.map { outfit ->
                                            if (outfit.itemIds.contains(item.id)) {
                                                val newItemIds = outfit.itemIds.toMutableList()
                                                newItemIds.remove(item.id)
                                                outfit.copy(itemIds = newItemIds)
                                            } else {
                                                outfit
                                            }
                                        }
                                        outfitsVm.outfits.clear()
                                        outfitsVm.outfits.addAll(updatedOutfits)
                                        outfitsVm.saveOutfitsToStorage(context)
                                    }
                                    wardrobeVm.removeItem(item.id, context)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = SecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        if (photoToView != null) {
            FullscreenImage(photoToView) { photoToView = null }
        }
    }
}

/* ---------------- ADD ITEM ---------------- */

@Composable
fun AddItemScreen(
    wardrobeVm: WardrobeViewModel,
    context: Context,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("Демисезон") }
    var color by remember { mutableStateOf(PrimaryColor) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var colorDropdownExpanded by remember { mutableStateOf(false) }

    val seasons = listOf("Демисезон", "Зима", "Лето")
    val gradientPalette = listOf(
        Pair(Brush.horizontalGradient(listOf(Color(0xFF6C5CE7), Color(0xFFB37FF2))), Color(0xFF6C5CE7)),
        Pair(Brush.horizontalGradient(listOf(Color(0xFFFF7675), Color(0xFFFFA6A6))), Color(0xFFFF7675)),
        Pair(Brush.horizontalGradient(listOf(Color(0xFF81ECEC), Color(0xFF74B9FF))), Color(0xFF81ECEC)),
        Pair(Brush.horizontalGradient(listOf(Color(0xFFFFEAA7), Color(0xFFFFC312))), Color(0xFFFFEAA7)),
        Pair(Brush.horizontalGradient(listOf(Color(0xFF55EFC4), Color(0xFF00B894))), Color(0xFF55EFC4)),
        Pair(Brush.horizontalGradient(listOf(Color(0xFFFF9F43), Color(0xFFFF6B6B))), Color(0xFFFF9F43)),
        Pair(Brush.horizontalGradient(listOf(Color(0xFF2D3436), Color(0xFF636E72))), Color(0xFF2D3436))
    )

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri = uri
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название вещи") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Цвет")
        Box {
            Button(
                onClick = { colorDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = OnPrimaryColor)
            ) {
                Text("Выбери цвет")
            }

            DropdownMenu(
                expanded = colorDropdownExpanded,
                onDismissRequest = { colorDropdownExpanded = false },
                modifier = Modifier.background(SurfaceColor)
            ) {
                gradientPalette.forEach { (gradient, mainColor) ->
                    DropdownMenuItem(
                        text = {},
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(gradient, CircleShape)
                                    .border(
                                        width = if (color == mainColor) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                            )
                        },
                        onClick = {
                            color = mainColor
                            colorDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Сезон года")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            seasons.forEach { s ->
                FilterChip(
                    selected = season == s,
                    onClick = { season = s },
                    label = { Text(s) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = OnPrimaryColor)
        ) {
            Text("Выбрать фото")
        }

        if (photoUri != null) {
            Image(
                painter = rememberAsyncImagePainter(photoUri),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    var photoFileName: String? = null

                    // Сохраняем фото во внутреннее хранилище
                    photoUri?.let { uri ->
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            photoFileName = "photo_${System.currentTimeMillis()}.jpg"
                            val file = File(context.filesDir, photoFileName)
                            FileOutputStream(file).use { outputStream ->
                                inputStream?.copyTo(outputStream)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    wardrobeVm.addItem(
                        WardrobeItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            season = season,
                            colorValue = color.toLongValue(),
                            photoFileName = photoFileName
                        ),
                        context
                    )
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor, contentColor = OnPrimaryColor)
        ) {
            Text("СОХРАНИТЬ ВЕЩЬ")
        }
    }
}

/* ---------------- OUTFITS SCREEN ---------------- */

@Composable
fun OutfitsScreen(
    wardrobeVm: WardrobeViewModel,
    outfitsVm: OutfitsViewModel,
    context: Context
) {
    var outfitName by remember { mutableStateOf("") }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    var clickedPhoto by remember { mutableStateOf<Uri?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Создать новый образ", fontSize = 20.sp, color = PrimaryColor)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = outfitName,
                onValueChange = { outfitName = it },
                label = { Text("Название образа") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("Выбери вещи для образа:", fontSize = 16.sp, color = PrimaryColor)
        }

        items(wardrobeVm.items) { item ->
            val isSelected = selectedItemIds.contains(item.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedItemIds.remove(item.id)
                            else selectedItemIds.add(item.id)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ItemPhotoCard(
                        item = item,
                        context = context,
                        modifier = Modifier,
                        onClick = { uri -> clickedPhoto = uri }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontSize = 16.sp)
                        Text(item.season, color = SecondaryColor)
                    }
                    if (isSelected) {
                        Text("✓", color = SecondaryColor, fontSize = 20.sp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (outfitName.isNotBlank() && selectedItemIds.isNotEmpty()) {
                        outfitsVm.addOutfit(
                            Outfit(
                                id = UUID.randomUUID().toString(),
                                name = outfitName,
                                itemIds = selectedItemIds.toList()
                            ),
                            context
                        )
                        outfitName = ""
                        selectedItemIds.clear()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor, contentColor = OnPrimaryColor)
            ) {
                Text("СОХРАНИТЬ ОБРАЗ")
            }
            Spacer(Modifier.height(24.dp))
            Text("Сохранённые образы:", fontSize = 18.sp, color = PrimaryColor)
            Spacer(Modifier.height(12.dp))
        }

        items(outfitsVm.outfits) { outfit ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(outfit.name, fontSize = 16.sp, color = SecondaryColor)
                        IconButton(
                            onClick = {
                                outfitsVm.removeOutfit(outfit.id, context)
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить образ",
                                tint = SecondaryColor
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        outfit.itemIds.forEach { itemId ->
                            val item = wardrobeVm.items.find { it.id == itemId }
                            if (item != null) {
                                ItemPhotoCard(
                                    item = item,
                                    context = context,
                                    modifier = Modifier.size(50.dp),
                                    onClick = { uri -> clickedPhoto = uri }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (clickedPhoto != null) {
        FullscreenImage(clickedPhoto) { clickedPhoto = null }
    }
}