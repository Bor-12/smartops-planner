package com.smartops.planner.config;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.user.Role;
import com.smartops.planner.user.User;
import com.smartops.planner.user.UserRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DemoDataLoader implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(
            UserRepository userRepository,
            SkillRepository skillRepository,
            EmployeeRepository employeeRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createUserIfMissing("admin", Role.ADMIN);
        createUserIfMissing("manager", Role.MANAGER);
        createUserIfMissing("employee", Role.EMPLOYEE);
        createUserIfMissing("laura.sanchez", Role.MANAGER);
        createUserIfMissing("miguel.torres", Role.MANAGER);
        createUserIfMissing("ana.garcia", Role.EMPLOYEE);
        createUserIfMissing("carlos.martin", Role.EMPLOYEE);
        createUserIfMissing("marta.lopez", Role.EMPLOYEE);
        createUserIfMissing("david.romero", Role.EMPLOYEE);
        createUserIfMissing("elena.navarro", Role.EMPLOYEE);
        createUserIfMissing("pablo.ruiz", Role.EMPLOYEE);

        Map<String, Skill> skills = createSkills();
        createEmployees(skills);
        createTasks(skills);
    }

    private void createUserIfMissing(String username, Role role) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        userRepository.save(new User(username, passwordEncoder.encode(DEMO_PASSWORD), role));
    }

    private Map<String, Skill> createSkills() {
        for (String skillName : List.of(
                "Java",
                "Spring Boot",
                "REST API",
                "SQL",
                "PostgreSQL",
                "JPA",
                "Security",
                "Testing",
                "Docker",
                "GitHub Actions",
                "CI/CD",
                "Monitoring",
                "HTML",
                "CSS",
                "JavaScript",
                "UX",
                "Analytics",
                "Reporting",
                "Documentation"
        )) {
            if (!skillRepository.existsByNameIgnoreCase(skillName)) {
                skillRepository.save(new Skill(skillName));
            }
        }

        return skillRepository.findAll()
                .stream()
                .collect(Collectors.toMap(skill -> key(skill.getName()), Function.identity(), (first, second) -> first));
    }

    private void createEmployees(Map<String, Skill> skills) {
        createEmployeeIfMissing(
                "Ana Garcia",
                "ana.garcia@smartops.demo",
                40,
                10,
                SeniorityLevel.SENIOR,
                skills,
                "Java",
                "Spring Boot",
                "SQL",
                "PostgreSQL",
                "JPA"
        );
        createEmployeeIfMissing(
                "Carlos Martin",
                "carlos.martin@smartops.demo",
                35,
                22,
                SeniorityLevel.SENIOR,
                skills,
                "Java",
                "Testing",
                "Security",
                "REST API"
        );
        createEmployeeIfMissing(
                "Marta Lopez",
                "marta.lopez@smartops.demo",
                40,
                18,
                SeniorityLevel.SENIOR,
                skills,
                "Security",
                "Testing",
                "Documentation",
                "Java"
        );
        createEmployeeIfMissing(
                "Javier Moreno",
                "javier.moreno@smartops.demo",
                32,
                24,
                SeniorityLevel.SENIOR,
                skills,
                "Java",
                "Spring Boot",
                "REST API",
                "Documentation"
        );
        createEmployeeIfMissing(
                "David Romero",
                "david.romero@smartops.demo",
                40,
                32,
                SeniorityLevel.MID,
                skills,
                "Docker",
                "GitHub Actions",
                "CI/CD",
                "Monitoring"
        );
        createEmployeeIfMissing(
                "Elena Navarro",
                "elena.navarro@smartops.demo",
                38,
                20,
                SeniorityLevel.MID,
                skills,
                "Java",
                "SQL",
                "JPA",
                "Testing"
        );
        createEmployeeIfMissing(
                "Pablo Ruiz",
                "pablo.ruiz@smartops.demo",
                35,
                30,
                SeniorityLevel.MID,
                skills,
                "Docker",
                "PostgreSQL",
                "Monitoring"
        );
        createEmployeeIfMissing(
                "Lucia Fernandez",
                "lucia.fernandez@smartops.demo",
                40,
                12,
                SeniorityLevel.MID,
                skills,
                "JavaScript",
                "HTML",
                "CSS",
                "UX"
        );
        createEmployeeIfMissing(
                "Raquel Serrano",
                "raquel.serrano@smartops.demo",
                40,
                28,
                SeniorityLevel.MID,
                skills,
                "Security",
                "Monitoring",
                "CI/CD"
        );
        createEmployeeIfMissing(
                "Sergio Prieto",
                "sergio.prieto@smartops.demo",
                30,
                8,
                SeniorityLevel.JUNIOR,
                skills,
                "Java",
                "Testing"
        );
        createEmployeeIfMissing(
                "Nuria Campos",
                "employee@smartops.demo",
                30,
                6,
                SeniorityLevel.JUNIOR,
                skills,
                "Java",
                "Testing",
                "Documentation"
        );
        createEmployeeIfMissing(
                "Alvaro Vega",
                "alvaro.vega@smartops.demo",
                30,
                14,
                SeniorityLevel.JUNIOR,
                skills,
                "HTML",
                "CSS",
                "JavaScript"
        );
        createEmployeeIfMissing(
                "Irene Molina",
                "irene.molina@smartops.demo",
                30,
                10,
                SeniorityLevel.JUNIOR,
                skills,
                "SQL",
                "Analytics",
                "Reporting"
        );
        createEmployeeIfMissing(
                "Diego Herrera",
                "diego.herrera@smartops.demo",
                30,
                26,
                SeniorityLevel.JUNIOR,
                skills,
                "Docker",
                "GitHub Actions"
        );
        createEmployeeIfMissing(
                "Carla Medina",
                "carla.medina@smartops.demo",
                32,
                16,
                SeniorityLevel.JUNIOR,
                skills,
                "Testing",
                "Documentation"
        );
        createEmployeeIfMissing(
                "Belen Ortega",
                "belen.ortega@smartops.demo",
                35,
                18,
                SeniorityLevel.MID,
                skills,
                "Reporting",
                "Analytics",
                "Documentation",
                "UX"
        );
    }

    private void createEmployeeIfMissing(
            String name,
            String email,
            Integer maxWeeklyHours,
            Integer currentWeeklyHours,
            SeniorityLevel seniorityLevel,
            Map<String, Skill> skills,
            String... skillNames
    ) {
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Employee employee = new Employee(name, email, maxWeeklyHours, seniorityLevel);
        employee.setCurrentWeeklyHours(currentWeeklyHours);
        employee.setSkills(resolveSkills(skills, skillNames));
        employeeRepository.save(employee);
    }

    private void createTasks(Map<String, Skill> skills) {
        Set<String> existingTaskTitles = taskRepository.findAll()
                .stream()
                .map(task -> key(task.getTitle()))
                .collect(Collectors.toSet());

        createTaskIfMissing(
                existingTaskTitles,
                "Reforzar seguridad del acceso",
                TaskPriority.HIGH,
                5,
                LocalDate.now().plusDays(3),
                skills,
                "Security",
                "Testing"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Corregir incidencia del panel operativo",
                TaskPriority.HIGH,
                4,
                LocalDate.now().plusDays(2),
                skills,
                "Java",
                "Spring Boot",
                "REST API"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Optimizar consultas de PostgreSQL",
                TaskPriority.HIGH,
                6,
                LocalDate.now().plusDays(5),
                skills,
                "SQL",
                "PostgreSQL"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Mejorar puntuacion de asignaciones",
                TaskPriority.HIGH,
                7,
                LocalDate.now().plusDays(6),
                skills,
                "Java",
                "Testing"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Anadir registro de auditoria",
                TaskPriority.HIGH,
                5,
                LocalDate.now().plusDays(4),
                skills,
                "Security",
                "JPA"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Crear panel de planificacion",
                TaskPriority.MEDIUM,
                6,
                LocalDate.now().plusDays(7),
                skills,
                "Java",
                "Spring Boot"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Preparar despliegue con Docker",
                TaskPriority.MEDIUM,
                3,
                LocalDate.now().plusDays(14),
                skills,
                "Docker"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Configurar pipeline de GitHub Actions",
                TaskPriority.MEDIUM,
                4,
                LocalDate.now().plusDays(10),
                skills,
                "GitHub Actions",
                "CI/CD"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Crear informe de carga de trabajo",
                TaskPriority.MEDIUM,
                4,
                LocalDate.now().plusDays(8),
                skills,
                "Reporting",
                "Analytics"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Mejorar interfaz del dashboard",
                TaskPriority.MEDIUM,
                5,
                LocalDate.now().plusDays(9),
                skills,
                "HTML",
                "CSS",
                "JavaScript",
                "UX"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Escribir tests de integracion",
                TaskPriority.MEDIUM,
                4,
                LocalDate.now().plusDays(12),
                skills,
                "Testing",
                "Spring Boot"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Anadir ejemplos a OpenAPI",
                TaskPriority.MEDIUM,
                3,
                LocalDate.now().plusDays(11),
                skills,
                "Documentation",
                "REST API"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Anadir endpoint de monitorizacion",
                TaskPriority.MEDIUM,
                4,
                LocalDate.now().plusDays(13),
                skills,
                "Monitoring",
                "Spring Boot"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Refactorizar mapeo de DTOs",
                TaskPriority.LOW,
                3,
                LocalDate.now().plusDays(16),
                skills,
                "Java"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Mejorar documentacion del README",
                TaskPriority.LOW,
                2,
                LocalDate.now().plusDays(18),
                skills,
                "Documentation"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Anadir mensajes cuando no haya datos",
                TaskPriority.LOW,
                2,
                LocalDate.now().plusDays(15),
                skills,
                "UX",
                "JavaScript"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Limpiar CSS no utilizado",
                TaskPriority.LOW,
                2,
                LocalDate.now().plusDays(20),
                skills,
                "CSS"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Anadir contadores SQL al dashboard",
                TaskPriority.LOW,
                3,
                LocalDate.now().plusDays(17),
                skills,
                "SQL",
                "Reporting"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Mejorar textos de las asignaciones",
                TaskPriority.LOW,
                2,
                LocalDate.now().plusDays(19),
                skills,
                "Documentation",
                "UX"
        );
        createTaskIfMissing(
                existingTaskTitles,
                "Revisar logs de integracion continua",
                TaskPriority.LOW,
                2,
                LocalDate.now().plusDays(21),
                skills,
                "CI/CD",
                "GitHub Actions"
        );
    }

    private void createTaskIfMissing(
            Set<String> existingTaskTitles,
            String title,
            TaskPriority priority,
            Integer estimatedHours,
            LocalDate deadline,
            Map<String, Skill> skills,
            String... requiredSkillNames
    ) {
        if (existingTaskTitles.contains(key(title))) {
            return;
        }

        Task task = new Task(title, priority, estimatedHours);
        task.setDeadline(deadline);
        task.setRequiredSkills(resolveSkills(skills, requiredSkillNames));
        taskRepository.save(task);
        existingTaskTitles.add(key(title));
    }

    private HashSet<Skill> resolveSkills(Map<String, Skill> skills, String... names) {
        return java.util.Arrays.stream(names)
                .map(name -> skills.get(key(name)))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
