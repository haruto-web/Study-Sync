package com.example.studysync_project.utils;

import com.example.studysync_project.ui.home.ReadyModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReadyModuleCatalog {

    private ReadyModuleCatalog() {}

    public static List<ReadyModule> getAllModules() {
        // Keep modules short to reduce prompt size; this is app content, not user data.
        List<ReadyModule> modules = new ArrayList<>();

        // Philippines Grades 1–10 starter set (3 modules)
        modules.add(new ReadyModule(
                "ph_g1_eng_phonics",
                "Alphabet Recognition and Phonics",
                "Grade 1",
                "English",
                "Phonics",
                "Learn letter names, sounds, and simple blending.",
                "Alphabet and phonics help you read by matching letters to sounds.\n\nExamples:\n- The letter A can sound like /a/ as in apple.\n- The letter B sounds like /b/ as in ball.\n\nBlending (put sounds together):\n- c + a + t → cat\n- b + a + t → bat\n\nTip: Say each sound slowly, then blend faster to read the whole word."
        ));

        modules.add(new ReadyModule(
                "ph_g1_math_count_add",
                "Counting Numbers and Basic Addition",
                "Grade 1",
                "Mathematics",
                "Counting & Addition",
                "Count objects and add small numbers.",
                "Counting means saying numbers in order: 1, 2, 3, 4, 5...\n\nAddition means putting groups together.\n\nExamples:\n- 2 + 1 = 3\n- 3 + 2 = 5\n- 4 + 0 = 4\n\nNumber sentence idea: Start with a number, then count forward to add.\nExample: 3 + 2 → start at 3, count 2 more: 4, 5 → answer is 5."
        ));

        modules.add(new ReadyModule(
                "ph_g1_sci_living_nonliving",
                "Living and Nonliving Things",
                "Grade 1",
                "Science",
                "Living vs Nonliving",
                "Sort things into living and nonliving and explain why.",
                "Living things are alive. They need air, water, and food, and they grow.\n\nNonliving things are not alive.\n\nExamples:\n- Living: dog, tree, fish, human\n- Nonliving: rock, chair, pencil, toy car\n\nCheck questions:\n1) Does it grow?\n2) Does it need food or water?\n3) Does it breathe?\nIf yes to many, it is probably living."
        ));

        // Grade 2
        modules.add(new ReadyModule(
                "ph_g2_eng_simple_sentences",
                "Reading Simple Sentences",
                "Grade 2",
                "English",
                "Simple Sentences",
                "Read and understand short sentences.",
                "A sentence is a group of words that tells a complete idea.\n\nExamples:\n- I see a cat.\n- The sun is hot.\n- We can run fast.\n\nTips:\n1) Point to each word as you read.\n2) Look for the period (.) at the end.\n3) Ask: Who? What happened?"
        ));
        modules.add(new ReadyModule(
                "ph_g2_math_sub_patterns",
                "Subtraction and Number Patterns",
                "Grade 2",
                "Mathematics",
                "Subtraction & Patterns",
                "Subtract within 100 and spot simple patterns.",
                "Subtraction means taking away.\n\nExamples:\n- 12 − 5 = 7\n- 30 − 10 = 20\n\nNumber patterns repeat or grow in a rule.\nExamples:\n- 2, 4, 6, 8 (add 2)\n- 10, 9, 8, 7 (subtract 1)"
        ));
        modules.add(new ReadyModule(
                "ph_g2_sci_plants_animals",
                "Plants and Animals",
                "Grade 2",
                "Science",
                "Plants & Animals",
                "Identify basic needs and parts of plants/animals.",
                "Plants need sunlight, water, air, and space to grow.\nAnimals need food, water, air, and shelter.\n\nPlant parts:\n- Roots: take in water\n- Stem: holds plant up\n- Leaves: make food\n\nAnimals use body parts (legs, wings, fins) to move and survive."
        ));
        modules.add(new ReadyModule(
                "ph_g2_fil_pagbasa_salita",
                "Pagbasa ng mga Salita",
                "Grade 2",
                "Filipino",
                "Pagbasa",
                "Basahin ang mga salitang pantig at pangungusap.",
                "Ang pagbasa ay pagkilala sa mga titik at tunog upang mabuo ang salita.\n\nHalimbawa:\n- ba + sa = basa\n- ma + ga = maga\n\nTip: Basahin ang bawat pantig, pagkatapos ay buuin ang salita."
        ));
        modules.add(new ReadyModule(
                "ph_g2_ap_family_community",
                "My Family and Community",
                "Grade 2",
                "Araling Panlipunan",
                "Pamilya at Komunidad",
                "Unawain ang papel ng pamilya at komunidad.",
                "Ang pamilya ay unang pangkat na kinabibilangan natin.\nAng komunidad ay lugar kung saan tayo nakatira at nakikisalamuha.\n\nHalimbawa ng tungkulin:\n- Magulang: gumagabay\n- Bata: tumutulong at sumusunod\n\nMahalaga ang paggalang, pagtutulungan, at pagiging responsable."
        ));

        // Grade 3
        modules.add(new ReadyModule(
                "ph_g3_eng_parts_of_speech",
                "Parts of Speech (Nouns, Verbs, Adjectives)",
                "Grade 3",
                "English",
                "Parts of Speech",
                "Identify nouns, verbs, and adjectives.",
                "Noun: a person, place, or thing (teacher, Manila, book).\nVerb: an action word (run, write, jump).\nAdjective: describes a noun (big, happy, red).\n\nExample:\n“The happy dog runs.”\n- dog = noun\n- runs = verb\n- happy = adjective"
        ));
        modules.add(new ReadyModule(
                "ph_g3_math_mult_div_intro",
                "Multiplication and Division",
                "Grade 3",
                "Mathematics",
                "Multiplication & Division",
                "Use repeated addition and sharing equally.",
                "Multiplication is repeated addition.\nExample: 3 × 4 = 4 + 4 + 4 = 12\n\nDivision is sharing equally.\nExample: 12 ÷ 3 = 4 (12 shared into 3 equal groups).\n\nFact family:\n3 × 4 = 12\n4 × 3 = 12\n12 ÷ 3 = 4\n12 ÷ 4 = 3"
        ));
        modules.add(new ReadyModule(
                "ph_g3_sci_human_body",
                "Human Body Systems",
                "Grade 3",
                "Science",
                "Human Body",
                "Learn basic body parts and their functions.",
                "The human body has parts that work together.\n\nExamples:\n- Brain: controls actions and thinking\n- Heart: pumps blood\n- Lungs: help us breathe\n- Stomach: helps digest food\n\nHealthy habits: eat balanced food, sleep well, and exercise."
        ));
        modules.add(new ReadyModule(
                "ph_g3_fil_pangngalan_panghalip",
                "Pangngalan at Panghalip",
                "Grade 3",
                "Filipino",
                "Pangngalan/Panghalip",
                "Kilalanin ang pangngalan at panghalip.",
                "Pangngalan: ngalan ng tao, hayop, bagay, lugar (Ana, aso, mesa, Cebu).\nPanghalip: pamalit sa pangngalan (siya, nila, kami).\n\nHalimbawa:\nSi Ana ay nagbabasa. Siya ay masipag.\n- Ana = pangngalan\n- Siya = panghalip"
        ));
        modules.add(new ReadyModule(
                "ph_g3_ap_symbols_culture",
                "Philippine Symbols and Culture",
                "Grade 3",
                "Araling Panlipunan",
                "Simbolo at Kultura",
                "Kilalanin ang mga simbolo at kulturang Pilipino.",
                "Mga simbolo ng bansa:\n- Watawat\n- Pambansang Awit\n- Pambansang Selyo\n\nKultura: paraan ng pamumuhay (pagkain, kasuotan, tradisyon).\nMahalaga ang paggalang sa iba’t ibang kultura sa Pilipinas."
        ));

        // Grade 4
        modules.add(new ReadyModule(
                "ph_g4_eng_subject_verb_agreement",
                "Subject-Verb Agreement",
                "Grade 4",
                "English",
                "Grammar",
                "Match subjects and verbs correctly.",
                "A subject and verb must agree in number.\n\nSingular subject → singular verb:\n- He runs.\n- The dog barks.\n\nPlural subject → plural verb:\n- They run.\n- The dogs bark.\n\nTip: If the subject is one (he/she/it), often add -s to the verb in present tense."
        ));
        modules.add(new ReadyModule(
                "ph_g4_math_fractions_decimals",
                "Fractions and Decimals",
                "Grade 4",
                "Mathematics",
                "Fractions & Decimals",
                "Represent parts of a whole and simple decimal forms.",
                "A fraction shows parts of a whole: 1/2, 3/4.\nA decimal is another way to write fractions: 0.5, 0.75.\n\nExamples:\n- 1/2 = 0.5\n- 1/4 = 0.25\n\nCompare by using the same denominator or turning into decimals."
        ));
        modules.add(new ReadyModule(
                "ph_g4_sci_matter_properties",
                "Matter and Its Properties",
                "Grade 4",
                "Science",
                "Matter",
                "Describe solids, liquids, gases, and properties.",
                "Matter is anything that has mass and takes up space.\n\nStates of matter:\n- Solid: fixed shape (rock)\n- Liquid: takes container shape (water)\n- Gas: spreads out (air)\n\nProperties:\n- Color, texture, hardness, and how it changes with heat."
        ));
        modules.add(new ReadyModule(
                "ph_g4_fil_pandiwa_panguri",
                "Pandiwa at Pang-uri",
                "Grade 4",
                "Filipino",
                "Pandiwa/Pang-uri",
                "Kilalanin ang pandiwa at pang-uri.",
                "Pandiwa: kilos o gawain (tumakbo, kumain, nagsulat).\nPang-uri: naglalarawan (mabait, mabilis, maganda).\n\nHalimbawa:\nMabilis tumakbo ang bata.\n- tumakbo = pandiwa\n- mabilis = pang-uri"
        ));
        modules.add(new ReadyModule(
                "ph_g4_ap_maps_directions",
                "Maps and Directions",
                "Grade 4",
                "Araling Panlipunan",
                "Mapa at Direksyon",
                "Gamitin ang mapa at pangunahing direksyon.",
                "Ang mapa ay larawan ng lugar.\nPangunahing direksyon: Hilaga, Timog, Silangan, Kanluran.\n\nMga simbolo at legend: tumutulong maintindihan ang mapa.\nScale: nagpapakita kung gaano kalayo sa tunay na sukat."
        ));

        // Grade 5
        modules.add(new ReadyModule(
                "ph_g5_eng_reading_strategies",
                "Reading Comprehension Strategies",
                "Grade 5",
                "English",
                "Reading Comprehension",
                "Use strategies to understand a text.",
                "Strategies:\n1) Preview: look at title and headings\n2) Ask questions: Who/What/Why/How\n3) Summarize each paragraph\n4) Find keywords and context clues\n\nAfter reading, retell the story using your own words."
        ));
        modules.add(new ReadyModule(
                "ph_g5_math_measurement",
                "Measurement (Length, Mass, Time)",
                "Grade 5",
                "Mathematics",
                "Measurement",
                "Convert units and solve measurement problems.",
                "Common units:\n- Length: cm, m, km\n- Mass: g, kg\n- Time: seconds, minutes, hours\n\nExamples:\n- 100 cm = 1 m\n- 60 minutes = 1 hour\n\nUse conversion factors and check if your answer makes sense."
        ));
        modules.add(new ReadyModule(
                "ph_g5_sci_force_motion",
                "Force and Motion",
                "Grade 5",
                "Science",
                "Force & Motion",
                "Explain pushes/pulls and how objects move.",
                "A force is a push or pull.\n\nMotion is a change in position.\n\nExamples:\n- Pushing a cart makes it move forward.\n- Friction slows objects down.\n\nKey idea: Stronger force can change speed or direction more."
        ));
        modules.add(new ReadyModule(
                "ph_g5_fil_pagunawa_binasa",
                "Pag-unawa sa Binasa",
                "Grade 5",
                "Filipino",
                "Pag-unawa",
                "Sagot sa tanong at pagkuha ng detalye sa teksto.",
                "Pagkatapos magbasa, sagutin ang tanong:\n- Sino? Ano? Saan? Kailan? Bakit? Paano?\n\nHanapin ang pangunahing ideya at mahahalagang detalye.\nGamitin ang konteksto para maintindihan ang mga bagong salita."
        ));
        modules.add(new ReadyModule(
                "ph_g5_ap_phil_history_intro",
                "Philippine History",
                "Grade 5",
                "Araling Panlipunan",
                "Kasaysayan",
                "Panimula sa mahahalagang pangyayari sa kasaysayan.",
                "Ang kasaysayan ay pag-aaral ng mga pangyayari sa nakaraan.\n\nHalimbawa ng paksa:\n- Mga sinaunang pamayanan\n- Panahon ng kolonisasyon\n- Mahahalagang pagbabago sa lipunan\n\nGamitin ang timeline para makita ang pagkakasunod-sunod."
        ));

        // Grade 6
        modules.add(new ReadyModule(
                "ph_g6_eng_writing_paragraphs",
                "Writing Paragraphs and Short Essays",
                "Grade 6",
                "English",
                "Writing",
                "Write clear paragraphs with a main idea.",
                "A paragraph has:\n- Topic sentence (main idea)\n- Supporting details\n- Closing sentence\n\nShort essay tip: Plan with an outline (intro, body, conclusion).\nUse transition words: first, next, because, however, therefore."
        ));
        modules.add(new ReadyModule(
                "ph_g6_math_geometry",
                "Geometry (Shapes and Angles)",
                "Grade 6",
                "Mathematics",
                "Geometry",
                "Identify shapes and measure angles.",
                "Basic shapes: triangle, square, rectangle, circle.\nAngles are measured in degrees (°).\n\nAngle types:\n- Acute: < 90°\n- Right: = 90°\n- Obtuse: > 90°\n\nTriangle angle sum is 180°."
        ));
        modules.add(new ReadyModule(
                "ph_g6_sci_earth_space",
                "Earth and Space",
                "Grade 6",
                "Science",
                "Earth & Space",
                "Learn about Earth, the Moon, and the Sun.",
                "Earth rotates (day and night) and revolves around the Sun (years).\n\nThe Moon revolves around Earth and affects tides.\n\nSeasons relate to Earth’s tilt and its orbit around the Sun."
        ));
        modules.add(new ReadyModule(
                "ph_g6_fil_pagsulat_talata",
                "Pagsulat ng Talata",
                "Grade 6",
                "Filipino",
                "Pagsulat",
                "Sumulat ng talatang may paksa at detalye.",
                "Ang talata ay may:\n- Pangunahing ideya\n- Suportang detalye\n- Pangwakas\n\nGamitin ang mga salitang nag-uugnay: una, sunod, dahil dito, sa wakas."
        ));
        modules.add(new ReadyModule(
                "ph_g6_ap_heroes",
                "Heroes of the Philippines",
                "Grade 6",
                "Araling Panlipunan",
                "Mga Bayani",
                "Kilalanin ang mga bayani at kanilang ambag.",
                "Ang bayani ay taong naglingkod at nag-alay para sa bansa.\n\nHalimbawa ng pag-aaral:\n- Sino siya?\n- Ano ang ginawa niya?\n- Bakit mahalaga ang kanyang ambag?\n\nIugnay ang mga bayani sa panahon at pangyayari sa kasaysayan."
        ));

        // Grade 7
        modules.add(new ReadyModule(
                "ph_g7_eng_main_idea_details",
                "Identifying Main Idea and Supporting Details",
                "Grade 7",
                "English",
                "Main Idea",
                "Find the main point and evidence in a text.",
                "Main idea: what the text is mostly about.\nSupporting details: facts/examples that prove it.\n\nSteps:\n1) Read the paragraph\n2) Ask: what is the author’s point?\n3) Highlight details that support it\n\nTip: Repeated ideas often reveal the main idea."
        ));
        modules.add(new ReadyModule(
                "ph_g7_math_integers_rationals",
                "Integers and Rational Numbers",
                "Grade 7",
                "Mathematics",
                "Integers & Rational Numbers",
                "Work with negative numbers and fractions.",
                "Integers include negative numbers, zero, and positive numbers: …, −2, −1, 0, 1, 2…\nRational numbers can be written as a fraction a/b.\n\nRules:\n- Adding a negative is like subtracting\n- Subtracting a negative becomes adding\n\nExample: 5 + (−3) = 2; 4 − (−2) = 6"
        ));
        modules.add(new ReadyModule(
                "ph_g7_sci_weather_climate",
                "Weather and Climate",
                "Grade 7",
                "Science",
                "Weather & Climate",
                "Differentiate weather from climate and read basic data.",
                "Weather is day-to-day conditions (rainy today).\nClimate is long-term pattern (usually hot and humid).\n\nTools:\n- Thermometer (temperature)\n- Rain gauge (rainfall)\n\nClimate factors include latitude, altitude, and nearby water."
        ));
        modules.add(new ReadyModule(
                "ph_g7_fil_wastong_bantas",
                "Wastong Gamit ng Bantas",
                "Grade 7",
                "Filipino",
                "Bantas",
                "Gamitin ang tuldok, kuwit, tandang pananong at padamdam.",
                "Mga bantas:\n- Tuldok (.) tapos na ideya\n- Kuwit (,) paghinto o paghihiwalay ng ideya\n- Tandang pananong (?) tanong\n- Tandang padamdam (!) matinding damdamin\n\nHalimbawa:\nKumain ka na ba?\nAng saya!"
        ));
        modules.add(new ReadyModule(
                "ph_g7_ap_government_citizenship",
                "Government and Citizenship",
                "Grade 7",
                "Araling Panlipunan",
                "Pamahalaan",
                "Unawain ang tungkulin ng pamahalaan at mamamayan.",
                "Ang pamahalaan ay nagtatakda ng batas at serbisyo.\nAng mamamayan ay may karapatan at responsibilidad.\n\nResponsibilidad:\n- Sumunod sa batas\n- Makilahok sa komunidad\n- Bumoto kapag nasa tamang edad"
        ));

        // Grade 8
        modules.add(new ReadyModule(
                "ph_g8_eng_grammar_punctuation",
                "Grammar Rules and Punctuation",
                "Grade 8",
                "English",
                "Grammar & Punctuation",
                "Use correct punctuation and avoid common grammar errors.",
                "Common punctuation:\n- Period (.) ends a statement\n- Comma (,) separates items/clauses\n- Apostrophe (') shows possession (Ana’s book)\n\nCommon fixes:\n- Don’t mix tenses\n- Avoid run-on sentences\n- Use commas after intro phrases"
        ));
        modules.add(new ReadyModule(
                "ph_g8_math_algebraic_expressions",
                "Algebraic Expressions",
                "Grade 8",
                "Mathematics",
                "Algebra",
                "Translate words to expressions and simplify.",
                "An algebraic expression uses numbers, variables, and operations.\n\nExamples:\n- “3 more than x” → x + 3\n- “twice a number n” → 2n\n\nSimplify by combining like terms:\n2x + 3x = 5x\n4a − a = 3a"
        ));
        modules.add(new ReadyModule(
                "ph_g8_sci_ecosystems",
                "Ecosystems and Environment",
                "Grade 8",
                "Science",
                "Ecosystems",
                "Explain food chains and human impact.",
                "An ecosystem includes living things and their environment.\n\nFood chain example:\nplant → insect → frog → snake\n\nRoles:\n- Producer: makes food (plants)\n- Consumer: eats others\n- Decomposer: breaks down dead matter\n\nHuman actions can help (recycling) or harm (pollution)."
        ));
        modules.add(new ReadyModule(
                "ph_g8_fil_pagsasalaysay",
                "Pagsasalaysay ng Kuwento",
                "Grade 8",
                "Filipino",
                "Kuwento",
                "Bumuo ng kuwentong may simula, gitna, at wakas.",
                "Ang pagsasalaysay ay pagkuwento ng pangyayari.\n\nBahagi:\n- Simula: tauhan at tagpuan\n- Gitna: problema at pangyayari\n- Wakas: solusyon at aral\n\nGumamit ng malinaw na pagkakasunod-sunod ng pangyayari."
        ));
        modules.add(new ReadyModule(
                "ph_g8_ap_asean",
                "ASEAN Countries",
                "Grade 8",
                "Araling Panlipunan",
                "ASEAN",
                "Kilalanin ang mga bansa sa ASEAN at layunin nito.",
                "Ang ASEAN ay samahan ng mga bansa sa Timog-Silangang Asya.\nLayunin: kooperasyon sa ekonomiya, kultura, at kapayapaan.\n\nPag-aralan:\n- Lokasyon ng mga bansa\n- Pangunahing produkto\n- Ugnayan sa Pilipinas"
        ));

        // Grade 9
        modules.add(new ReadyModule(
                "ph_g9_eng_narrative_informative",
                "Writing Narrative and Informative Texts",
                "Grade 9",
                "English",
                "Writing",
                "Write narratives and informative pieces with structure.",
                "Narrative writing tells a story (characters, setting, conflict, resolution).\nInformative writing explains a topic using facts and examples.\n\nStructure tip:\n- Intro: hook + topic\n- Body: organized points\n- Conclusion: summary + takeaway"
        ));
        modules.add(new ReadyModule(
                "ph_g9_math_linear_equations",
                "Linear Equations",
                "Grade 9",
                "Mathematics",
                "Linear Equations",
                "Solve one- and two-step equations.",
                "Goal: isolate the variable.\n\nExamples:\n- x + 7 = 20 → x = 13\n- 3x = 18 → x = 6\n- 2x + 4 = 14 → 2x = 10 → x = 5\n\nCheck by substituting your answer back into the equation."
        ));
        modules.add(new ReadyModule(
                "ph_g9_sci_energy_electricity",
                "Energy and Electricity",
                "Grade 9",
                "Science",
                "Energy & Electricity",
                "Understand simple circuits and energy forms.",
                "Energy can be light, heat, sound, chemical, or electrical.\n\nElectric circuits need a power source, wires, and a load (like a bulb).\n\nBasic ideas:\n- Closed circuit: current flows\n- Open circuit: no flow\n- Conductors allow electricity; insulators block it"
        ));
        modules.add(new ReadyModule(
                "ph_g9_fil_pagbubuod",
                "Pagbubuod ng Teksto",
                "Grade 9",
                "Filipino",
                "Pagbubuod",
                "Ibuod ang teksto nang maikli at tama.",
                "Ang buod ay pinaikling bersyon ng teksto.\n\nHakbang:\n1) Hanapin ang pangunahing ideya\n2) Piliin ang mahahalagang detalye\n3) Isulat sa sariling salita\n4) Iwasan ang sobrang detalye at opinyon"
        ));
        modules.add(new ReadyModule(
                "ph_g9_ap_economy_trade",
                "Economy and Trade",
                "Grade 9",
                "Araling Panlipunan",
                "Ekonomiya",
                "Unawain ang pangangailangan, produkto, at kalakalan.",
                "Ang ekonomiya ay pag-aaral ng produksyon, pamamahagi, at pagkonsumo.\n\nKalakalan (trade) ay palitan ng produkto at serbisyo.\n\nKonsepto:\n- Supply at demand\n- Presyo at kita\n- Lokal at pandaigdigang kalakalan"
        ));

        // Grade 10
        modules.add(new ReadyModule(
                "ph_g10_eng_oral_comm",
                "Public Speaking and Oral Communication",
                "Grade 10",
                "English",
                "Oral Communication",
                "Deliver clear speeches with confidence.",
                "Public speaking basics:\n- Know your purpose and audience\n- Organize: intro, points, conclusion\n- Use clear voice and pace\n- Maintain eye contact\n\nPractice tip: rehearse aloud and time your speech."
        ));
        modules.add(new ReadyModule(
                "ph_g10_math_statistics_probability",
                "Statistics and Probability",
                "Grade 10",
                "Mathematics",
                "Statistics & Probability",
                "Summarize data and compute simple probabilities.",
                "Statistics describes data using:\n- Mean, median, mode\n- Range\n\nProbability:\nP(event) = favorable outcomes / total outcomes\n\nExample: If there are 2 red and 3 blue marbles,\nP(red) = 2/5."
        ));
        modules.add(new ReadyModule(
                "ph_g10_sci_investigation",
                "Scientific Investigation and Experimentation",
                "Grade 10",
                "Science",
                "Scientific Method",
                "Design simple investigations and interpret results.",
                "Steps:\n1) Ask a question\n2) Make a hypothesis\n3) Identify variables (independent, dependent, controlled)\n4) Collect data\n5) Analyze and conclude\n\nGood experiments change one variable at a time and record results clearly."
        ));
        modules.add(new ReadyModule(
                "ph_g10_fil_pagsulat_sanaysay",
                "Pagsulat ng Sanaysay",
                "Grade 10",
                "Filipino",
                "Sanaysay",
                "Sumulat ng sanaysay na may malinaw na punto.",
                "Ang sanaysay ay mahabang pagsulat tungkol sa isang paksa.\n\nBalangkas:\n- Panimula: paksa at pahayag\n- Katawan: mga dahilan at ebidensya\n- Wakas: buod at panghuling mensahe\n\nGumamit ng malinaw na halimbawa at maayos na pagkakasunod."
        ));
        modules.add(new ReadyModule(
                "ph_g10_ap_contemporary_issues",
                "Contemporary Issues in Society",
                "Grade 10",
                "Araling Panlipunan",
                "Kontemporaryong Isyu",
                "Suriin ang mga isyu gamit ang datos at pananaw.",
                "Kontemporaryong isyu: mga suliraning kinakaharap ngayon (hal. kahirapan, klima, disinformation).\n\nPag-aaral:\n- Ano ang sanhi?\n- Sino ang apektado?\n- Ano ang posibleng solusyon?\n\nGumamit ng mapagkakatiwalaang sanggunian at iwasan ang maling impormasyon."
        ));

        return Collections.unmodifiableList(modules);
    }
}
