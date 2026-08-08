package br.com.petingle.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.petingle.ui.screen.SplashScreen
import br.com.petingle.ui.screen.main.MainScreen
import br.com.petingle.ui.screen.main.diary.DiaryAddEntryScreen
import br.com.petingle.ui.screen.main.diary.DiaryEditEntryScreen
import br.com.petingle.ui.screen.main.pets.NewPetScreen
import br.com.petingle.ui.screen.main.pets.PetDetailScreen
import br.com.petingle.ui.screen.main.pets.PetPhotoEditorScreen
import br.com.petingle.ui.screen.main.reminders.NewReminderScreen
import br.com.petingle.ui.screen.onboarding.OnboardingScreen
import br.com.petingle.ui.viewmodel.AppViewModel
import br.com.petingle.ui.viewmodel.DiaryViewModel
import br.com.petingle.ui.viewmodel.NewPetViewModel
import br.com.petingle.ui.viewmodel.NewReminderViewModel
import br.com.petingle.ui.viewmodel.PetDetailViewModel
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Splash     : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Main       : Screen("main")

    // Diário — galeria → prévia + legenda + pet → salvar
    object DiaryAddEntry : Screen("diary_add_entry/{imageUri}") {
        fun createRoute(imageUri: Uri): String =
            "diary_add_entry/${URLEncoder.encode(imageUri.toString(), "UTF-8")}"
    }

    // Formulário "Novo Pet" — tela única (SPEC §11)
    object NewPet : Screen("new_pet")

    // Formulário "Editar Pet" — mesmo layout, mas pré-preenchido
    object EditPet : Screen("edit_pet/{petId}") {
        fun createRoute(petId: Long): String = "edit_pet/$petId"
    }

    // Editor de foto para o fluxo de criação de pet
    object PetPhotoEditor : Screen("pet_photo_editor/{imageUri}") {
        fun createRoute(imageUri: Uri): String =
            "pet_photo_editor/${URLEncoder.encode(imageUri.toString(), "UTF-8")}"
    }

    // Editor de foto para o fluxo de edição de pet
    object EditPetPhotoEditor : Screen("edit_pet_photo_editor/{imageUri}") {
        fun createRoute(imageUri: Uri): String =
            "edit_pet_photo_editor/${URLEncoder.encode(imageUri.toString(), "UTF-8")}"
    }

    // Novo Lembrete / Editar Lembrete — reminderId = -1 → criar; > 0 → editar
    object NewReminder : Screen("new_reminder/{reminderId}") {
        fun createRoute(reminderId: Long = -1L): String = "new_reminder/$reminderId"
    }

    // Detalhe do pet com sub-abas de saúde (SPEC §12)
    object PetDetail : Screen("pet_detail/{petId}") {
        fun createRoute(petId: Long): String = "pet_detail/$petId"
    }

    // Editar entrada do Diário (legenda + pet, foto inalterada)
    object DiaryEditEntry : Screen("diary_edit_entry/{entryId}") {
        fun createRoute(entryId: Long): String = "diary_edit_entry/$entryId"
    }
}

@Composable
fun PetIngleNavGraph() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val isOnboardingDone by appViewModel.isOnboardingDone.collectAsState()
    val isReady          by appViewModel.isReady.collectAsState()

    // Duração padrão das transições de slide (sem fade — offset 100% elimina sobreposição)
    val slideDuration = 280

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route,
        // Navegar PARA frente: nova tela entra pela direita (offset = largura total)
        enterTransition  = { slideInHorizontally(tween(slideDuration)) { it } },
        // Navegar PARA frente: tela atual sai pela esquerda (offset = largura total)
        exitTransition   = { slideOutHorizontally(tween(slideDuration)) { -it } },
        // Voltar (popBackStack): tela anterior entra pela esquerda
        popEnterTransition = { slideInHorizontally(tween(slideDuration)) { -it } },
        // Voltar (popBackStack): tela atual sai pela direita
        popExitTransition  = { slideOutHorizontally(tween(slideDuration)) { it } },
    ) {

        // Splash sem transição (primeira tela — não há tela anterior)
        composable(
            route          = Screen.Splash.route,
            enterTransition = { slideInHorizontally(tween(slideDuration)) { it } },
            exitTransition  = { slideOutHorizontally(tween(slideDuration)) { -it } },
        ) {
            SplashScreen(
                isReady    = isReady,
                onNavigate = {
                    val dest = if (isOnboardingDone) Screen.Main.route else Screen.Onboarding.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        // ── Casca de navegação global (5 abas) ──────────────────────────────
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToDiaryPhotoEditor = { imageUri ->
                    navController.navigate(Screen.DiaryAddEntry.createRoute(imageUri))
                },
                onNavigateToNewPet = {
                    navController.navigate(Screen.NewPet.route)
                },
                onNavigateToEditPet = { petId ->
                    navController.navigate(Screen.EditPet.createRoute(petId))
                },
                onNavigateToNewReminder = { reminderId ->
                    navController.navigate(Screen.NewReminder.createRoute(reminderId))
                },
                onNavigateToPetDetail = { petId ->
                    navController.navigate(Screen.PetDetail.createRoute(petId))
                },
                onNavigateToEditDiaryEntry = { entryId ->
                    navController.navigate(Screen.DiaryEditEntry.createRoute(entryId))
                },
            )
        }

        // ── Novo Pet (tela única) ────────────────────────────────────────────
        composable(Screen.NewPet.route) { entry ->
            val newPetViewModel: NewPetViewModel = hiltViewModel(entry)
            NewPetScreen(
                viewModel  = newPetViewModel,
                petId      = -1L,
                onDismiss  = { navController.popBackStack() },
                onSaved    = { navController.popBackStack() },
                onNavigateToPhotoEditor = { imageUri ->
                    navController.navigate(Screen.PetPhotoEditor.createRoute(imageUri))
                },
            )
        }

        // ── Editor de foto (fluxo Novo Pet) ─────────────────────────────────
        composable(
            route     = Screen.PetPhotoEditor.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri").orEmpty()
            val imageUri   = Uri.parse(URLDecoder.decode(encodedUri, "UTF-8"))
            val newPetEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.NewPet.route)
            }
            val newPetViewModel: NewPetViewModel = hiltViewModel(newPetEntry)
            PetPhotoEditorScreen(
                imageUri  = imageUri,
                onDismiss = { navController.popBackStack() },
                onSave    = { photoPath ->
                    newPetViewModel.setPhotoPath(photoPath)
                    navController.popBackStack()
                },
            )
        }

        // ── Editar Pet (tela única, pré-preenchida) ──────────────────────────
        composable(
            route     = Screen.EditPet.route,
            arguments = listOf(navArgument("petId") { type = NavType.LongType }),
        ) { entry ->
            val petId          = entry.arguments?.getLong("petId") ?: return@composable
            val editPetViewModel: NewPetViewModel = hiltViewModel(entry)
            NewPetScreen(
                viewModel  = editPetViewModel,
                petId      = petId,
                onDismiss  = { navController.popBackStack() },
                onSaved    = { navController.popBackStack() },
                onNavigateToPhotoEditor = { imageUri ->
                    navController.navigate(Screen.EditPetPhotoEditor.createRoute(imageUri))
                },
            )
        }

        // ── Editor de foto (fluxo Editar Pet) ───────────────────────────────
        composable(
            route     = Screen.EditPetPhotoEditor.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri").orEmpty()
            val imageUri   = Uri.parse(URLDecoder.decode(encodedUri, "UTF-8"))
            // Partilha o ViewModel escopado à rota EditPet pai.
            // getBackStackEntry com o padrão de rota encontra a entrada corretamente.
            val editPetEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.EditPet.route)
            }
            val editPetViewModel: NewPetViewModel = hiltViewModel(editPetEntry)
            PetPhotoEditorScreen(
                imageUri  = imageUri,
                onDismiss = { navController.popBackStack() },
                onSave    = { photoPath ->
                    editPetViewModel.setPhotoPath(photoPath)
                    navController.popBackStack()
                },
            )
        }

        // ── Nova entrada do Diário ───────────────────────────────────────────
        composable(
            route     = Screen.DiaryAddEntry.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedUri   = backStackEntry.arguments?.getString("imageUri").orEmpty()
            val imageUri     = Uri.parse(URLDecoder.decode(encodedUri, "UTF-8"))
            val diaryViewModel: DiaryViewModel = hiltViewModel()
            val pets by diaryViewModel.pets.collectAsState()
            DiaryAddEntryScreen(
                imageUri  = imageUri,
                pets      = pets,
                onDismiss = { navController.popBackStack() },
                onSave    = { petId, photoPath, caption ->
                    diaryViewModel.addEntry(petId = petId, photoPath = photoPath, caption = caption)
                    navController.popBackStack()
                },
            )
        }

        // ── Novo Lembrete / Editar Lembrete ─────────────────────────────────
        composable(
            route     = Screen.NewReminder.route,
            arguments = listOf(
                navArgument("reminderId") { type = NavType.LongType; defaultValue = -1L }
            ),
        ) { backStackEntry ->
            val reminderId              = backStackEntry.arguments?.getLong("reminderId") ?: -1L
            val newReminderViewModel: NewReminderViewModel = hiltViewModel(backStackEntry)
            NewReminderScreen(
                reminderId = reminderId,
                viewModel  = newReminderViewModel,
                onDismiss  = { navController.popBackStack() },
                onSaved    = { navController.popBackStack() },
            )
        }

        // ── Detalhe do pet — sub-abas de saúde (SPEC §12) ───────────────────
        composable(
            route     = Screen.PetDetail.route,
            arguments = listOf(navArgument("petId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val petDetailViewModel: PetDetailViewModel = hiltViewModel(backStackEntry)
            PetDetailScreen(
                viewModel = petDetailViewModel,
                onBack    = { navController.popBackStack() },
            )
        }

        // ── Editar entrada do Diário ─────────────────────────────────────────
        composable(
            route     = Screen.DiaryEditEntry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val entryId       = backStackEntry.arguments?.getLong("entryId") ?: return@composable
            val diaryViewModel: DiaryViewModel = hiltViewModel()
            val entry by diaryViewModel.getEntryById(entryId).collectAsState(initial = null)
            val pets  by diaryViewModel.pets.collectAsState()
            val e = entry ?: return@composable
            DiaryEditEntryScreen(
                entry     = e,
                pets      = pets,
                onDismiss = { navController.popBackStack() },
                onSave    = { updatedEntry ->
                    diaryViewModel.updateEntry(updatedEntry)
                    navController.popBackStack()
                },
            )
        }
    }
}
