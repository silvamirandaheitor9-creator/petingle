package br.com.petingle.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Raios de borda conforme SPEC seção 3 ─────────────────────────────────────
// 16dp em cards, 24dp em botões/pills, aplicado consistentemente em 100% do app.
val CardCornerRadius = 16.dp
val PillCornerRadius = 24.dp

// Shape tokens diretos, para uso explícito em cards, botões e pills.
val CardShape = RoundedCornerShape(CardCornerRadius)
val PillShape = RoundedCornerShape(PillCornerRadius)

// Shapes do MaterialTheme — mapeadas para que componentes padrão do Material3
// (Card, Button, etc.) sigam os raios do SPEC sem precisar de override manual
// em cada uso.
val PetIngleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = CardShape,   // usado por Card por padrão no Material3
    large      = PillShape,   // usado por Button/FilledButton por padrão no Material3
    extraLarge = RoundedCornerShape(28.dp),
)
