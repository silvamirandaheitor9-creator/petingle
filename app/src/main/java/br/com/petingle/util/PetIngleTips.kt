package br.com.petingle.util

import java.util.Calendar

/**
 * Banco de dicas de cuidado animal rotacionadas por dia do ano.
 * 30 dicas para Cão e Gato; 10+ para Pássaro, Peixe, Réptil e Roedor;
 * lista geral como fallback quando nenhum pet está cadastrado.
 */
object PetIngleTips {

    private val dogTips = listOf(
        "Cães precisam de água fresca disponível o dia todo — troque pelo menos duas vezes ao dia.",
        "Passeios diários reduzem ansiedade e comportamentos destrutivos em cães de qualquer porte.",
        "Escove os dentes do seu cão pelo menos três vezes por semana para prevenir tártaro e gengivite.",
        "A dose de ração ideal varia com peso, idade e nível de atividade — consulte o veterinário regularmente.",
        "Cães idosos precisam de checkups semestrais mesmo quando parecem saudáveis.",
        "Brinquedos de mastigar ajudam a limpar os dentes e reduzem o estresse do seu cão.",
        "Evite passear nas horas mais quentes: o asfalto pode queimar as patinhas acima de 40 °C.",
        "Socialização precoce (até 16 semanas) é crucial para um cão equilibrado e confiante.",
        "Cães precisam de identidade visual: sempre coloque coleira com plaquinha e contato.",
        "O microchip é a forma mais segura de identificação — pergunte ao veterinário sobre o procedimento.",
        "Pulgas e carrapatos transmitem doenças sérias: mantenha a antipulga em dia o ano inteiro.",
        "Cães que ficam sozios por mais de 8 horas tendem a desenvolver ansiedade de separação.",
        "Enriquecimento ambiental (quebra-cabeças, mordedores, Kong) ocupa a mente e evita tédio.",
        "Nunca dê uva, chocolate, cebola ou xilitol ao seu cão — são tóxicos mesmo em pequenas quantidades.",
        "A vacina antirrábica é obrigatória por lei e protege tanto o pet quanto as pessoas ao redor.",
        "Banho frequente demais resseca a pele — o ideal para a maioria das raças é a cada 15-30 dias.",
        "Cães de raças braquicefálicas (Bulldog, Pug) sofrem mais com calor e esforço físico intenso.",
        "Adestramento positivo com reforço de recompensa é mais eficaz e gera menos estresse que punição.",
        "A vermifugação deve ser feita a cada 3-6 meses, dependendo do ambiente e hábitos do pet.",
        "Camas e cobertores próprios dão segurança e ajudam o cão a dormir melhor.",
        "Visitas ao veterinário para peso e pressão arterial são importantes mesmo fora das vacinas.",
        "Cães precisam de descanso: um adulto saudável dorme entre 12 e 14 horas por dia.",
        "Escovação regular evita nós no pelo e permite que você detecte parasitas ou feridas ocultas.",
        "Não ignore mudanças de apetite por mais de dois dias — podem indicar problemas de saúde.",
        "Cães são animais sociais: a companhia humana (ou de outro pet) faz bem à saúde emocional.",
        "O períneo (região sob a cauda) precisa ser higienizado regularmente para evitar infecções.",
        "Filhotes devem completar o esquema vacinal antes de contato com ambientes públicos.",
        "Patas rachadas no inverno podem ser tratadas com pomadas à base de lanolina ou vaselina.",
        "Exercício mental (aprender comandos, farejamento) cansa tanto quanto exercício físico.",
        "Um cão bem estimulado latia menos, dorme melhor e é mais fácil de conviver.",
    )

    private val catTips = listOf(
        "Gatos precisam de água fresca longe do pote de comida — o instinto selvagem os afasta de água perto do alimento.",
        "Arranhadores não são luxo: são essenciais para manter as unhas saudáveis e marcar território.",
        "A caixa de areia deve ser limpa diariamente — gatos limpos evitam a caixa suja e fazem necessidade fora.",
        "Um gato que para de comer por mais de 24 horas precisa de atenção veterinária imediata.",
        "Gatos são mestres em esconder dor — consultas regulares detectam problemas antes dos sintomas.",
        "A castração reduz o risco de câncer de mama e útero em fêmeas, e tumores testiculares em machos.",
        "Brincar 15-20 minutos por dia reduz ansiedade e mantém o peso saudável de gatos de interior.",
        "Nunca dê acetaminofeno (paracetamol) a gatos — é extremamente tóxico para eles.",
        "Gatos lambem muito pelo: bolas de pelo frequentes indicam necessidade de escovação mais regular.",
        "A altura das camas e arranhadores importa: gatos adoram pontos altos para observar o ambiente.",
        "Plantas como lírio, philodendro e espada-de-são-jorge são tóxicas para gatos.",
        "Gatos idosos acima de 7 anos devem ter checkup semestral com exame de sangue completo.",
        "A dieta úmida (sachê ou lata) ajuda na hidratação — gatos naturalmente bebem pouca água.",
        "Introduza um gato novo ao lar aos poucos: comece por um cômodo isolado e vá ampliando o espaço.",
        "Dentes de gato acumulam tártaro rapidamente — escovação semanal previne periodontite.",
        "Gatos com acesso à rua têm expectativa de vida menor — ambiente interno é mais seguro.",
        "Ronronar é comunicação, não só felicidade: gatos também ronronam quando ansiosos ou com dor.",
        "Microchip é fundamental para gatos de interior que podem escapar por janelas ou portas.",
        "Caixinhas de papelão, sacolas e esconderijos simples satisfazem o instinto de tocas.",
        "Gatos sentem estresse com mudanças de rotina — mantenha horários de refeição estáveis.",
        "Vermifugação regular é necessária mesmo para gatos que ficam em casa.",
        "Unhas longas podem se enrolar e causar lesões — corte a cada 2-3 semanas com alicate próprio.",
        "Gatos de pelagem longa precisam de escovação diária para evitar nós e bolas de pelo.",
        "A frequência urinária do gato é um sinal de saúde importante — note qualquer mudança.",
        "Felinway (feromonas sintéticas) ajuda gatos ansiosos em mudanças, viagens e multicat.",
        "Gatos dormem de 12 a 16 horas por dia — isso é completamente normal e saudável.",
        "Evite aromas fortes de citros e eucalipto perto do gato — eles são naturalmente repelentes.",
        "A vacinação quádrupla (ou tripla) protege de doenças graves como a cinomose felina.",
        "Janelas teladas permitem ventilação e estimulação sensorial sem risco de quedas.",
        "Um gato entediado pode desenvolver comportamentos repetitivos — varie os brinquedos com frequência.",
    )

    private val birdTips = listOf(
        "Pássaros precisam de luz natural ou lâmpada UVB por pelo menos 4 horas diárias.",
        "A gaiola deve ser grande o suficiente para o pássaro abrir as asas completamente sem tocar as grades.",
        "Nunca use spray de cozinha ou panelas de teflon próximo de pássaros — os vapores são letais.",
        "Pássaros socializam muito: se ficar muitas horas sozinho, considere adotar um par.",
        "A dieta deve ir além de sementes — frutas, folhas e ração granulada equilibram os nutrientes.",
        "Água fresca e limpa deve ser trocada diariamente; pássaros são sensíveis a contaminação.",
        "Penas arrepiadas por muito tempo podem indicar frio, doença ou estresse — observe com atenção.",
        "Banhos regulares (spray ou recipiente raso) são importantes para a saúde das penas.",
        "Objetos brilhantes, espelhos e brinquedos variados evitam tédio e depenamento.",
        "Veterinário especializado em aves é essencial — nem todo clínico tem experiência com pássaros.",
        "Evite correntes de ar direto na gaiola: pássaros são muito sensíveis a variações de temperatura.",
        "O corte das unhas e do bico deve ser feito por profissional para evitar lesões.",
    )

    private val fishTips = listOf(
        "Teste a qualidade da água semanalmente: amônia, nitrito e pH fora do ideal causam mortes silenciosas.",
        "Nunca coloque peixes novos direto no aquário — aclimate o saco por 20-30 min antes de abrir.",
        "Troca parcial de 20-30% da água a cada semana mantém o equilíbrio biológico do aquário.",
        "O filtro deve ser limpo com a própria água do aquário para não destruir as bactérias benéficas.",
        "Superalimentação é a causa mais comum de morte de peixes — alimente uma vez ao dia, o que comerem em 2 min.",
        "Peixes de água quente precisam de termostato calibrado — variações de temperatura estressam e matam.",
        "Plantas aquáticas naturais oxigenam a água e reduzem o crescimento de algas.",
        "Quarentena de 2 semanas para peixes novos evita introdução de doenças no aquário principal.",
        "Lâmpada acesa por mais de 10-12 horas por dia favorece proliferação excessiva de algas.",
        "Peixes doentes devem ser isolados imediatamente para evitar contágio dos demais.",
        "O tamanho do aquário importa: regra básica é 1 litro de água para cada centímetro de peixe.",
        "Decorações com bordas afiadas podem ferir as nadadeiras — prefira pedras lisas e plantas macias.",
    )

    private val reptileTips = listOf(
        "Répteis são ectotérmicos: precisam de gradiente térmico no terrário (área quente e fria) para regular a temperatura.",
        "Lâmpada UVB é essencial para a maioria dos répteis diurnos — sem ela, desenvolvem deficiência de cálcio.",
        "A umidade do terrário deve ser monitorada com higrômetro — varia muito por espécie.",
        "Répteis em muda precisam de banho morno para facilitar a troca da pele.",
        "Nunca manipule um réptil logo após a alimentação — aguarde pelo menos 48 horas.",
        "Substrato inadequado pode causar impactação intestinal — pesquise a espécie antes de escolher.",
        "Répteis escondem sinais de doença até estar gravemente doentes — checkups preventivos são essenciais.",
        "A água do bebedouro deve ser declorada (deixe descansar 24h ou use filtro) antes de oferecer.",
        "Viveiros de vidro retêm calor mais que telas — verifique a temperatura regularmente.",
        "Veterinário especializado em répteis e exóticos é indispensável — a medicina de répteis é muito específica.",
        "A dieta varia muito por espécie: pesquise se é insetívoro, herbívoro ou carnívoro antes de adotar.",
        "Enriquecimento ambiental (esconderijos, galhos, substrato para escavar) reduz estresse.",
    )

    private val rodentTips = listOf(
        "Roedores são animais sociais — ham-hams e ratos se dão melhor em pares ou grupos do mesmo sexo.",
        "A gaiola deve ter espaço para correr, escavar e esconder comida — é comportamento natural.",
        "Roda de exercícios deve ter superfície sólida (sem grades) para não machucar as patinhas.",
        "Roedores roem por necessidade fisiológica — ofereça blocos de madeira ou osso de sépias.",
        "Cama de maravalha de cedro ou pinho contém óleos que prejudicam o fígado dos roedores — prefira papel.",
        "A temperatura ideal para a maioria dos roedores é entre 18 °C e 24 °C — evite correntes de ar.",
        "Coberturas de metal são mais seguras que as de plástico — roedores roem e podem escapar.",
        "Hamsters são animais solitários e territoriais — briga entre dois numa mesma gaiola pode ser fatal.",
        "A limpeza da gaiola deve ser parcial para manter o cheiro familiar e reduzir o estresse do animal.",
        "Dentes que crescem demais (maloclusão) são comuns em roedores e precisam de atenção veterinária.",
        "Roedores têm metabolismo acelerado e adoecem rapidamente — não adie a consulta em caso de sintomas.",
        "Ofereça esconderijos e tocas: dormir em local seguro é essencial para o bem-estar desses animais.",
    )

    private val generalTips = listOf(
        "Todo pet merece visita ao veterinário pelo menos uma vez ao ano, mesmo sem sinais de doença.",
        "Identidade visual (coleira + plaquinha ou microchip) é o primeiro passo para recuperar um pet perdido.",
        "Água fresca disponível o dia todo é a necessidade mais básica de qualquer animal doméstico.",
        "Castração aumenta a expectativa de vida e reduz doenças graves em cães e gatos.",
        "Ambiente enriquecido com brinquedos e estímulos adequados previne comportamentos destrutivos.",
        "Mude a dieta do pet sempre com transição gradual (7 dias) para evitar problemas digestivos.",
        "Registre vacinas, vermifugações e consultas em um caderno ou app — o histórico é valioso.",
        "Pets idosos precisam de cuidados específicos — fale com o veterinário sobre exames preventivos.",
        "Sinais de dor em animais são sutis: observe mudanças de comportamento, apetite e postura.",
        "Uma rotina estável de alimentação e passeios dá segurança emocional ao seu pet.",
    )

    // Mapeia o storageValue da espécie (lowercase) para a lista de dicas.
    private val speciesTips: Map<String, List<String>> = mapOf(
        "cão"      to dogTips,
        "gato"     to catTips,
        "pássaro"  to birdTips,
        "passaro"  to birdTips,
        "peixe"    to fishTips,
        "réptil"   to reptileTips,
        "reptil"   to reptileTips,
        "roedor"   to rodentTips,
    )

    /**
     * Retorna a dica do dia para a espécie informada.
     * Se [species] for nulo, vazio ou não reconhecido, usa a lista geral.
     * A rotação é feita por [Calendar.DAY_OF_YEAR] para trocar automaticamente a cada dia.
     */
    fun getTodayTip(species: String?): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val key = species?.trim()?.lowercase()
        val tips = speciesTips[key] ?: generalTips
        return tips[dayOfYear % tips.size]
    }
}
