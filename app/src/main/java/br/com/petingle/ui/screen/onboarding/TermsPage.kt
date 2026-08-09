package br.com.petingle.ui.screen.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.petingle.ui.theme.OrangePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Textos legais completos ──────────────────────────────────────────────────

private val PRIVACY_TEXT = """
Política de Privacidade do PetIngle
Última atualização: Julho de 2026

1. SOBRE ESTE DOCUMENTO
Esta Política de Privacidade descreve como o aplicativo PetIngle trata as informações dos seus usuários. Ao usar o app, você concorda com as práticas descritas aqui.

2. DADOS QUE FICAM NO SEU APARELHO
O PetIngle não exige criação de conta nem coleta dados pessoais em servidores próprios. Todas as informações que você cadastrar — nomes dos pets, fotos, datas, registros de saúde (vacinas, consultas, peso, medicamentos, alimentação), entradas do Diário e lembretes — ficam armazenadas exclusivamente no seu dispositivo.

3. BACKUP E EXPORTAÇÃO
O app oferece função de backup manual. O arquivo gerado é salvo na pasta que você escolher no próprio dispositivo. Você é responsável pela guarda e segurança desse arquivo.

4. PERMISSÕES UTILIZADAS
• Câmera: usada apenas quando você decide fotografar um pet diretamente pelo app.
• Galeria / Armazenamento: usada para selecionar fotos existentes no dispositivo.
• Notificações: usadas para enviar lembretes de vacinas, consultas e outros cuidados que você cadastrar. Você pode desativar notificações a qualquer momento nas configurações do sistema.
Nenhuma permissão é solicitada antes do momento em que você realmente precisa dela.

5. PUBLICIDADE E SERVIÇOS DE TERCEIROS
O PetIngle exibe anúncios por meio da plataforma Start.io. Para disponibilizar, personalizar, limitar a frequência e medir o desempenho dos anúncios, a Start.io e seus parceiros podem tratar dados técnicos do aparelho e da utilização da publicidade, conforme as políticas próprias desses serviços. O PetIngle não envia para a Start.io os nomes, fotos, registros de saúde ou lembretes cadastrados no app.

Algumas áreas do app podem mostrar anúncios na parte inferior da tela para manter a experiência gratuita. Quando o usuário atingir o limite inicial de 10 perfis de pets, poderá assistir voluntariamente a um anúncio recompensado para desbloquear mais 5 perfis. Cada desbloqueio depende da disponibilidade e da conclusão válida do anúncio, e o procedimento pode ser repetido enquanto essa opção estiver disponível.

6. DADOS E RASTREAMENTO
O PetIngle não utiliza ferramentas próprias de análise de comportamento ou rastreamento de usuário. A publicidade pode utilizar identificadores e dados técnicos tratados pela Start.io, de acordo com as permissões, configurações do aparelho e políticas aplicáveis.

7. CRIANÇAS
O PetIngle não é destinado a crianças menores de 13 anos. Não coletamos intencionalmente informações de menores.

8. SEUS DIREITOS (LGPD — LEI 13.709/2018)
Em conformidade com a Lei Geral de Proteção de Dados, você tem direito a:
• Confirmar a existência de tratamento de dados;
• Acessar, corrigir ou excluir seus dados (feito diretamente no app);
• Solicitar a portabilidade ou eliminação dos dados;
• Revogar consentimentos a qualquer momento.
Como todos os dados ficam no seu dispositivo, você exerce esses direitos diretamente pelo app ou desinstalando-o.

9. ALTERAÇÕES NESTA POLÍTICA
Podemos atualizar esta política periodicamente. Alterações relevantes serão comunicadas dentro do próprio app. A data de "última atualização" no topo sempre reflete a versão vigente.
""".trimIndent()

private val TERMS_TEXT = """
Termos de Uso do PetIngle
Última atualização: Julho de 2026

1. ACEITAÇÃO DOS TERMOS
Ao instalar ou usar o PetIngle, você concorda com estes Termos de Uso. Se não concordar, não utilize o aplicativo.

2. DESCRIÇÃO DO SERVIÇO
O PetIngle é um aplicativo de organização pessoal para tutores de animais de estimação. Permite cadastrar pets, registrar histórico de saúde, criar lembretes e manter um diário fotográfico — tudo armazenado localmente no seu dispositivo.

3. PUBLICIDADE E DESBLOQUEIO DE PERFIS
O PetIngle é disponibilizado com anúncios exibidos pela plataforma Start.io. Os anúncios podem aparecer em espaços reservados na parte inferior das abas, sem impedir o uso das funções principais. Ao atingir o limite inicial de 10 perfis de pets, o usuário poderá assistir voluntariamente a um anúncio recompensado para liberar mais 5 perfis. Esse desbloqueio pode ser repetido, conforme a disponibilidade do anúncio.

4. NÃO SUBSTITUI VETERINÁRIO
As funcionalidades do PetIngle — incluindo campos de saúde, lembretes e registros — têm finalidade exclusivamente organizacional. O app não oferece diagnósticos, prescrições ou orientações médico-veterinárias. Consulte sempre um médico-veterinário habilitado para decisões sobre a saúde dos seus pets.

5. RESPONSABILIDADES DO USUÁRIO
• Você é responsável pela veracidade das informações cadastradas.
• Você é responsável por realizar backups regulares dos seus dados.
• Você concorda em usar o app somente para fins lícitos e pessoais.
• Não é permitido usar o app para fins comerciais sem autorização expressa.

6. PROPRIEDADE INTELECTUAL
O nome "PetIngle", o mascote, o design, os ícones, os textos e demais elementos visuais são propriedade exclusiva dos criadores do app. É vedada a reprodução, cópia ou uso comercial sem autorização prévia por escrito.

6. LIMITAÇÃO DE RESPONSABILIDADE
O PetIngle é fornecido "como está", sem garantias de disponibilidade ininterrupta ou ausência de erros. Não nos responsabilizamos por perdas de dados decorrentes de falhas no dispositivo, desinstalação do app ou ausência de backup.

7. MODIFICAÇÕES
Podemos alterar estes Termos a qualquer momento. O uso continuado do app após a publicação das alterações implica aceitação das novas condições. A data de "última atualização" no topo indica a versão vigente.

8. LEI APLICÁVEL
Estes Termos são regidos pelas leis da República Federativa do Brasil. Qualquer controvérsia será submetida ao foro da comarca do usuário, conforme o Código de Defesa do Consumidor.
""".trimIndent()

// ─── Itens do resumo visual ───────────────────────────────────────────────────

private data class BulletItem(val icon: ImageVector, val text: String)

private val BULLET_ITEMS = listOf(
    BulletItem(Icons.Outlined.Lock,
        "Seus dados ficam só no aparelho — nenhuma conta obrigatória."),
    BulletItem(Icons.Outlined.NotificationsNone,
        "Notificações chegam só quando você criar lembretes."),
)

// ─── Composable principal ─────────────────────────────────────────────────────

@Composable
fun TermsPage(
    isActive: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scrollState = rememberScrollState()
    val hasReachedEnd by remember { derivedStateOf { !scrollState.canScrollForward } }

    val alphas   = remember { List(BULLET_ITEMS.size) { Animatable(0f) } }
    val offsetsY = remember { List(BULLET_ITEMS.size) { Animatable(26f) } }

    LaunchedEffect(isActive) {
        if (isActive) {
            delay(200)
            BULLET_ITEMS.indices.forEach { i ->
                launch { alphas[i].animateTo(1f, animationSpec = tween(300)) }
                launch { offsetsY[i].animateTo(0f, animationSpec = tween(300)) }
                delay(120)
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        // Área com scroll
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Icon(
                imageVector        = Icons.Outlined.Shield,
                contentDescription = null,
                modifier           = Modifier.size(48.dp),
                tint               = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "Antes de começar",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "O PetIngle funciona sem cadastro. Veja como cuidamos dos seus dados e como o app funciona.",
                fontSize  = 14.sp,
                color     = Color.White.copy(alpha = 0.88f),
            )

            Spacer(Modifier.height(24.dp))

            BULLET_ITEMS.forEachIndexed { i, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .graphicsLayer {
                            alpha        = alphas[i].value
                            translationY = offsetsY[i].value
                        },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = null,
                        modifier           = Modifier.size(26.dp),
                        tint               = Color.White,
                    )
                    Text(
                        text     = item.text,
                        fontSize = 14.sp,
                        color    = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.20f))
            Spacer(Modifier.height(4.dp))

            TextButton(onClick = { showDialog = true }) {
                Text(
                    text  = "Ler o texto completo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(100.dp))
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        // Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked         = checked,
                onCheckedChange = { if (hasReachedEnd) onCheckedChange(it) },
                enabled         = hasReachedEnd,
                colors          = CheckboxDefaults.colors(
                    checkedColor            = Color.White,
                    checkmarkColor          = OrangePrimary,
                    uncheckedColor          = Color.White.copy(alpha = 0.70f),
                    disabledUncheckedColor  = Color.White.copy(alpha = 0.30f),
                ),
            )
            Text(
                text     = "Li e aceito os Termos de Uso e a Política de Privacidade.",
                fontSize = 13.sp,
                color    = if (hasReachedEnd) Color.White else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    if (showDialog) {
        FullTextDialog(onDismiss = { showDialog = false })
    }
}

// ─── Diálogo: texto completo ──────────────────────────────────────────────────

@Composable
private fun FullTextDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val contentScrollState = rememberScrollState()

    LaunchedEffect(selectedTab) { contentScrollState.scrollTo(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier      = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            shape         = MaterialTheme.shapes.medium,
            color         = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column {
                Text(
                    text     = "Documentos completos",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
                    color    = MaterialTheme.colorScheme.onSurface,
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = OrangePrimary,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        text     = { Text("Privacidade", style = MaterialTheme.typography.labelMedium) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        text     = { Text("Termos de Uso", style = MaterialTheme.typography.labelMedium) },
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(contentScrollState)
                        .padding(20.dp),
                ) {
                    Text(
                        text  = if (selectedTab == 0) PRIVACY_TEXT else TERMS_TEXT,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Fechar", color = OrangePrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
