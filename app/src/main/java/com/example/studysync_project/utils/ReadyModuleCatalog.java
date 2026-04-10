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

        // ── SHS STEM ──────────────────────────────────────────────────────────
        modules.add(shs("shs_stem_precalc", "Pre-Calculus Fundamentals",
                "Grade 11", "STEM", "Pre-Calculus", "Functions & Trigonometry",
                "Master functions, conic sections, and trigonometric identities.",
                "Pre-Calculus bridges algebra and calculus.\n\n" +
                "Functions:\n- A function maps each input to exactly one output.\n- Notation: f(x) = 2x + 1\n\n" +
                "Conic Sections:\n- Circle: (x-h)² + (y-k)² = r²\n- Parabola: y = ax² + bx + c\n- Ellipse and Hyperbola have two foci.\n\n" +
                "Trigonometry:\n- sin, cos, tan relate angles to side ratios in right triangles.\n- Unit circle: radius 1, angles in radians.\n- Key identity: sin²θ + cos²θ = 1\n\n" +
                "Practice: Sketch f(x) = x² − 4 and identify vertex, axis of symmetry, and intercepts.",
                "Intermediate",
                "• What is a Function?\n• Domain and Range\n• Conic Sections Overview\n• Trigonometric Ratios\n• Unit Circle and Radian Measure\n• Trigonometric Identities"));

        modules.add(shs("shs_stem_calculus", "Basic Calculus",
                "Grade 12", "STEM", "Basic Calculus", "Limits, Derivatives & Integrals",
                "Understand limits, differentiation, and basic integration.",
                "Calculus studies change and accumulation.\n\n" +
                "Limits:\n- lim(x→2) of x² = 4\n- Limits describe what a function approaches.\n\n" +
                "Derivatives (rate of change):\n- d/dx [xⁿ] = nxⁿ⁻¹  (Power Rule)\n- d/dx [x³] = 3x²\n- Derivative = slope of tangent line.\n\n" +
                "Integration (area under curve):\n- ∫xⁿ dx = xⁿ⁺¹/(n+1) + C\n- ∫x² dx = x³/3 + C\n\n" +
                "Fundamental Theorem: differentiation and integration are inverse operations.",
                "Advanced",
                "• Concept of Limits\n• Evaluating Limits Algebraically\n• Definition of the Derivative\n• Differentiation Rules\n• Applications of Derivatives\n• Introduction to Integration\n• Definite vs Indefinite Integrals"));

        modules.add(shs("shs_stem_stats", "Statistics and Probability",
                "Grade 11", "STEM", "Statistics and Probability", "Data Analysis",
                "Analyze data using descriptive statistics and probability rules.",
                "Statistics helps us make sense of data.\n\n" +
                "Descriptive Statistics:\n- Mean: sum ÷ count\n- Median: middle value\n- Mode: most frequent\n- Standard Deviation: spread of data\n\n" +
                "Probability:\n- P(A) = favorable / total outcomes\n- P(A or B) = P(A) + P(B) − P(A and B)\n- Independent events: P(A and B) = P(A) × P(B)\n\n" +
                "Normal Distribution: bell curve, 68% of data within 1 standard deviation of mean.",
                "Intermediate",
                "• Types of Data\n• Measures of Central Tendency\n• Measures of Spread\n• Basic Probability Rules\n• Conditional Probability\n• Normal Distribution"));

        modules.add(shs("shs_stem_gen_bio", "General Biology",
                "Grade 11", "STEM", "Science", "Cell Biology & Genetics",
                "Explore cell structure, DNA, and basic genetics.",
                "Biology is the study of life.\n\n" +
                "Cell Structure:\n- Cell membrane: controls what enters/exits\n- Nucleus: contains DNA\n- Mitochondria: produces energy (ATP)\n- Ribosomes: make proteins\n\n" +
                "DNA and Genetics:\n- DNA is a double helix made of nucleotides (A, T, G, C).\n- Genes are segments of DNA that code for traits.\n- Dominant allele (B) masks recessive allele (b).\n- Punnett square predicts offspring ratios.\n\n" +
                "Mitosis: cell division for growth (produces 2 identical cells).\nMeiosis: produces 4 sex cells with half the chromosomes.",
                "Intermediate",
                "• Cell Theory\n• Cell Organelles and Functions\n• DNA Structure and Replication\n• Mendelian Genetics\n• Punnett Squares\n• Mitosis vs Meiosis"));

        modules.add(shs("shs_stem_physics", "Physical Science: Physics",
                "Grade 11", "STEM", "Physical Science", "Motion & Forces",
                "Apply Newton's laws and kinematics to real-world problems.",
                "Physics explains how the physical world works.\n\n" +
                "Kinematics (motion without forces):\n- Speed = distance / time\n- Velocity includes direction\n- Acceleration = change in velocity / time\n\n" +
                "Newton's Laws:\n1) An object at rest stays at rest unless acted on by a force.\n2) F = ma (Force = mass × acceleration)\n3) Every action has an equal and opposite reaction.\n\n" +
                "Work and Energy:\n- Work = Force × distance\n- Kinetic Energy = ½mv²\n- Potential Energy = mgh",
                "Intermediate",
                "• Scalars vs Vectors\n• Kinematics Equations\n• Newton's First Law\n• Newton's Second Law (F=ma)\n• Newton's Third Law\n• Work, Energy, and Power"));

        // ── SHS ABM ──────────────────────────────────────────────────────────
        modules.add(shs("shs_abm_gen_math", "General Mathematics",
                "Grade 11", "ABM", "General Mathematics", "Functions & Business Math",
                "Apply functions, interest, and annuities to business contexts.",
                "General Mathematics connects algebra to real-world finance.\n\n" +
                "Functions:\n- Linear: f(x) = mx + b (straight line)\n- Quadratic: f(x) = ax² + bx + c (parabola)\n\n" +
                "Simple Interest: I = Prt\n- P = principal, r = rate, t = time\n- Example: P=10000, r=5%, t=2 years → I = 1000\n\n" +
                "Compound Interest: A = P(1 + r/n)^(nt)\n\n" +
                "Annuities: series of equal payments at regular intervals.\n- Used in loans, savings plans, and insurance.",
                "Beginner",
                "• Functions and Their Graphs\n• Simple Interest\n• Compound Interest\n• Annuities\n• Loans and Amortization\n• Basic Logic and Propositions"));

        modules.add(shs("shs_abm_fundamentals_acctg", "Fundamentals of Accountancy",
                "Grade 11", "ABM", "Fundamentals of Accountancy", "Accounting Cycle",
                "Learn the accounting equation, journals, and financial statements.",
                "Accounting records and reports financial transactions.\n\n" +
                "Accounting Equation: Assets = Liabilities + Owner's Equity\n\n" +
                "Steps in the Accounting Cycle:\n1) Identify transactions\n2) Record in journal (debit/credit)\n3) Post to ledger\n4) Prepare trial balance\n5) Prepare financial statements\n\n" +
                "Financial Statements:\n- Income Statement: revenues − expenses = net income\n- Balance Sheet: assets = liabilities + equity\n- Cash Flow Statement: tracks cash in and out.\n\n" +
                "Debit increases assets/expenses; Credit increases liabilities/equity.",
                "Beginner",
                "• Accounting Equation\n• Types of Accounts\n• Journal Entries (Debit & Credit)\n• Ledger and T-Accounts\n• Trial Balance\n• Income Statement\n• Balance Sheet"));

        modules.add(shs("shs_abm_business_math", "Business Mathematics",
                "Grade 11", "ABM", "Business Mathematics", "Profit, Loss & Markup",
                "Compute profit, loss, markup, markdown, and break-even.",
                "Business math applies arithmetic to commerce.\n\n" +
                "Key Formulas:\n- Profit = Revenue − Cost\n- Markup = Selling Price − Cost\n- Markup % = (Markup / Cost) × 100\n- Markdown = Original Price − Sale Price\n\n" +
                "Break-Even Point: when total revenue = total cost.\n- BEP (units) = Fixed Costs / (Price − Variable Cost per unit)\n\n" +
                "Percentage problems:\n- What is 15% of 2000? → 0.15 × 2000 = 300\n- 300 is what % of 2000? → (300/2000) × 100 = 15%",
                "Beginner",
                "• Fractions, Decimals, Percentages\n• Profit and Loss\n• Markup and Markdown\n• Break-Even Analysis\n• Payroll and Wages\n• Commissions and Discounts"));

        // ── SHS HUMSS ─────────────────────────────────────────────────────────
        modules.add(shs("shs_humss_oral_comm", "Oral Communication in Context",
                "Grade 11", "HUMSS", "Oral Communication", "Communication Models",
                "Understand communication models and deliver effective speeches.",
                "Communication is the exchange of information between sender and receiver.\n\n" +
                "Models of Communication:\n- Linear: sender → message → receiver\n- Interactive: adds feedback loop\n- Transactional: simultaneous sending and receiving\n\n" +
                "Elements: sender, message, channel, receiver, feedback, noise, context.\n\n" +
                "Speech Delivery Tips:\n- Articulate clearly and vary your pace\n- Use appropriate volume and tone\n- Maintain eye contact\n- Use gestures purposefully\n\n" +
                "Types of speeches: informative, persuasive, entertaining.",
                "Beginner",
                "• Nature and Elements of Communication\n• Models of Communication\n• Types of Communication\n• Verbal vs Non-Verbal\n• Barriers to Communication\n• Effective Speech Delivery"));

        modules.add(shs("shs_humss_reading_writing", "Reading and Writing Skills",
                "Grade 11", "HUMSS", "Reading and Writing", "Academic Writing",
                "Read critically and write well-structured academic texts.",
                "Academic reading and writing are core skills for higher education.\n\n" +
                "Critical Reading:\n- Identify the author's purpose and argument\n- Distinguish fact from opinion\n- Evaluate evidence and logical reasoning\n\n" +
                "Academic Writing Structure:\n- Introduction: background + thesis statement\n- Body: topic sentence + evidence + analysis\n- Conclusion: restate thesis + synthesis\n\n" +
                "Thesis Statement: one sentence that states your main argument.\nExample: 'Social media negatively affects student focus because it promotes constant distraction.'\n\n" +
                "Citation: always credit your sources (APA, MLA, Chicago).",
                "Intermediate",
                "• Active Reading Strategies\n• Identifying Thesis and Arguments\n• Fact vs Opinion\n• Writing a Thesis Statement\n• Paragraph Development\n• Academic Essay Structure\n• In-text Citation Basics"));

        modules.add(shs("shs_humss_ucsp", "Understanding Culture, Society and Politics",
                "Grade 11", "HUMSS", "Understanding Culture, Society and Politics", "Socialization & Culture",
                "Explore how culture, society, and politics shape human behavior.",
                "UCSP examines human life through anthropology, sociology, and political science.\n\n" +
                "Culture: shared beliefs, values, practices of a group.\n- Material culture: objects (tools, clothing)\n- Non-material culture: language, religion, norms\n\n" +
                "Socialization: process of learning norms and values.\n- Agents: family, school, peers, media\n\n" +
                "Social Stratification: ranking of people by wealth, power, prestige.\n\n" +
                "Government: system that makes and enforces rules.\n- Democracy, Monarchy, Dictatorship are types of government.",
                "Beginner",
                "• Anthropology, Sociology, Political Science\n• Culture and Its Elements\n• Socialization Agents\n• Social Groups and Institutions\n• Social Stratification\n• Forms of Government"));

        modules.add(shs("shs_humss_philo", "Introduction to Philosophy",
                "Grade 12", "HUMSS", "Introduction to Philosophy", "Logic & Ethics",
                "Think critically using philosophical reasoning and ethical frameworks.",
                "Philosophy is the love of wisdom — it asks fundamental questions.\n\n" +
                "Branches:\n- Metaphysics: nature of reality\n- Epistemology: nature of knowledge\n- Ethics: what is right and wrong\n- Logic: rules of correct reasoning\n\n" +
                "Logic:\n- Deductive: if premises are true, conclusion must be true.\n  Example: All humans are mortal. Socrates is human. ∴ Socrates is mortal.\n- Inductive: conclusion is probable based on evidence.\n\n" +
                "Ethical Theories:\n- Utilitarianism: greatest good for the greatest number\n- Deontology (Kant): duty-based ethics\n- Virtue Ethics: focus on character",
                "Intermediate",
                "• What is Philosophy?\n• Branches of Philosophy\n• Deductive and Inductive Reasoning\n• Logical Fallacies\n• Ethical Theories\n• Applying Ethics to Real Situations"));

        // ── SHS GAS ───────────────────────────────────────────────────────────
        modules.add(shs("shs_gas_media_info", "Media and Information Literacy",
                "Grade 11", "GAS", "Media and Information Literacy", "Media Literacy",
                "Evaluate media messages and use information responsibly.",
                "Media and Information Literacy (MIL) helps you navigate the information age.\n\n" +
                "Types of Media:\n- Print: newspapers, books\n- Broadcast: TV, radio\n- Digital/New Media: social media, websites\n\n" +
                "Evaluating Information (CRAAP Test):\n- Currency: is it recent?\n- Relevance: does it fit your need?\n- Authority: who wrote it?\n- Accuracy: is it supported by evidence?\n- Purpose: why was it created?\n\n" +
                "Disinformation vs Misinformation:\n- Misinformation: false info shared without intent to deceive\n- Disinformation: false info shared deliberately\n\n" +
                "Tip: Always cross-check with multiple credible sources.",
                "Beginner",
                "• What is Media Literacy?\n• Types of Media\n• Evaluating Sources (CRAAP Test)\n• Fake News and Disinformation\n• Responsible Social Media Use\n• Creating Responsible Content"));

        modules.add(shs("shs_gas_personal_dev", "Personal Development",
                "Grade 11", "GAS", "Personal Development", "Self-Awareness & Goal Setting",
                "Understand yourself and build habits for growth and success.",
                "Personal Development is about knowing yourself and growing intentionally.\n\n" +
                "Self-Awareness:\n- Know your strengths, weaknesses, values, and emotions.\n- Johari Window: a tool to understand self vs how others see you.\n\n" +
                "Emotional Intelligence (EQ):\n- Self-awareness, self-regulation, motivation, empathy, social skills.\n- High EQ leads to better relationships and decisions.\n\n" +
                "Goal Setting (SMART Goals):\n- Specific, Measurable, Achievable, Relevant, Time-bound\n- Example: 'I will study Math for 30 minutes every day for 2 weeks.'\n\n" +
                "Habits: small consistent actions compound into big results over time.",
                "Beginner",
                "• Self-Concept and Identity\n• Strengths and Weaknesses\n• Emotional Intelligence\n• Stress and Coping Strategies\n• SMART Goal Setting\n• Building Positive Habits"));

        // ── SHS TVL ───────────────────────────────────────────────────────────
        modules.add(shs("shs_tvl_ict_cs", "Computer Systems Servicing",
                "Grade 11", "TVL", "Science", "Hardware & Networking",
                "Learn PC hardware components, assembly, and basic networking.",
                "Computer Systems Servicing covers hardware, software, and networks.\n\n" +
                "Hardware Components:\n- CPU: processes instructions\n- RAM: temporary memory\n- HDD/SSD: permanent storage\n- Motherboard: connects all components\n- PSU: power supply unit\n\n" +
                "Assembly Steps:\n1) Ground yourself (anti-static)\n2) Install CPU on motherboard\n3) Insert RAM\n4) Mount motherboard in case\n5) Connect storage and power cables\n\n" +
                "Networking Basics:\n- LAN: local area network (home/school)\n- IP Address: unique identifier for each device\n- Router: directs traffic between networks\n- Protocols: TCP/IP, HTTP, FTP",
                "Beginner",
                "• Computer Hardware Overview\n• CPU, RAM, Storage\n• PC Assembly Steps\n• Operating System Basics\n• Network Types (LAN, WAN)\n• IP Addressing and Protocols"));

        modules.add(shs("shs_tvl_programming", "Introduction to Programming",
                "Grade 12", "TVL", "Science", "Programming Basics",
                "Write basic programs using variables, loops, and functions.",
                "Programming is giving instructions to a computer.\n\n" +
                "Core Concepts:\n- Variable: stores a value (int age = 16;)\n- Data Types: int, float, String, boolean\n- Operators: +, -, *, /, % (modulo)\n\n" +
                "Control Flow:\n- if/else: make decisions\n- for loop: repeat a fixed number of times\n- while loop: repeat while condition is true\n\n" +
                "Functions: reusable blocks of code.\n  void greet() { System.out.println(\"Hello!\"); }\n\n" +
                "Example (Java):\nfor (int i = 1; i <= 5; i++) {\n  System.out.println(i);\n}\nOutput: 1 2 3 4 5",
                "Beginner",
                "• What is Programming?\n• Variables and Data Types\n• Operators and Expressions\n• Conditional Statements (if/else)\n• Loops (for, while)\n• Functions/Methods\n• Basic Debugging"));

        return Collections.unmodifiableList(modules);
    }

    // Helper for SHS modules with strand, difficulty, and lessons
    private static ReadyModule shs(
            String id, String title, String grade, String strand,
            String subject, String topic, String description,
            String content, String difficulty, String lessons
    ) {
        return new ReadyModule(id, title, grade, strand, subject, topic, description, content, difficulty, lessons);
    }
}
