package basic.sprinng.pulsepoint.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * Serves the PulsePoint UI component documentation site.
 * Each component has its own documentation page at /components/{name}
 * with live previews, usage examples, and API reference.
 */
@Controller
@RequestMapping("/components")
public class ComponentDocsController {

        private final Map<String, Map<String, Object>> componentRegistry;
        private final List<Map<String, Object>> componentList;

        public ComponentDocsController() {
                this.componentRegistry = new LinkedHashMap<>();
                this.componentList = new ArrayList<>();
                initRegistry();
        }

        @GetMapping
        public String index(Model model) {
                model.addAttribute("components", componentList);
                model.addAttribute("categories", getCategorizedComponents());
                return "docs/components-index";
        }

        @GetMapping("/{componentName}")
        public String componentDocs(@PathVariable String componentName, Model model) {
                Map<String, Object> component = componentRegistry.get(componentName);
                if (component == null) {
                        return "redirect:/components";
                }
                model.addAttribute("component", component);
                model.addAttribute("allComponents", componentList);
                model.addAttribute("demoStatusList", List.of("TODO", "IN_PROGRESS", "DONE"));
                model.addAttribute("demoTableColumns", List.of("Name", "Status", "Priority"));
                model.addAttribute("demoTabList", List.of("Overview", "Settings", "Activity"));
                return "docs/component-detail";
        }

        private Map<String, List<Map<String, Object>>> getCategorizedComponents() {
                Map<String, List<Map<String, Object>>> categorized = new LinkedHashMap<>();
                for (Map<String, Object> comp : componentList) {
                        String category = (String) comp.get("category");
                        categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(comp);
                }
                return categorized;
        }

        @SuppressWarnings("unchecked")
        private void initRegistry() {
                // ── Form Components ──
                register("button", "Button", "Form",
                                "Displays a button or a component that looks like a button.",
                                List.of("default", "primary", "destructive", "outline", "ghost", "link", "icon"),
                                List.of(
                                                param("text", "String", "Button", true, "Button label text"),
                                                param("type", "String", "button", false,
                                                                "HTML button type (button/submit/reset)"),
                                                param("clickExpr", "String", "null", false,
                                                                "PulsePoint onclick expression"),
                                                param("disabled", "String", "null", false,
                                                                "State expression for disabled state"),
                                                param("loading", "String", "null", false,
                                                                "State expression for loading state"),
                                                param("loadingText", "String", "Loading...", false,
                                                                "Text shown during loading"),
                                                param("icon", "String", "null", false,
                                                                "SVG path d attribute for icon")),
                                "components/ui/button",
                                "<div th:replace=\"~{components/ui/button :: primary(text='Save Changes', loading='isSubmitting', loadingText='Saving...')}\"></div>",
                                """
                                                <template pp-component="form_1">
                                                  <div pp-component="form_1">
                                                    <div th:replace="~{components/ui/button :: primary(text='Submit', loading='isSaving', clickExpr='handleSubmit()')}"></div>
                                                    <script>
                                                      const [isSaving, setIsSaving] = pp.state(false);
                                                      const handleSubmit = async () => {
                                                        setIsSaving(true);
                                                        await pp.rpc("saveItem", {});
                                                        setIsSaving(false);
                                                      };
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                register("input", "Input", "Form",
                                "A text input field with label, placeholder, and validation error support.",
                                List.of("text", "textarea"),
                                List.of(
                                                param("id", "String", null, true, "HTML id attribute"),
                                                param("label", "String", null, true, "Label text above the input"),
                                                param("name", "String", null, true, "Form field name"),
                                                param("placeholder", "String", null, false, "Placeholder text"),
                                                param("errorExpr", "String", "null", false,
                                                                "State expression for validation error message")),
                                "components/ui/input",
                                "<div th:replace=\"~{components/ui/input :: text(id='email', label='Email', name='email', placeholder='Enter email...', errorExpr='emailError')}\"></div>",
                                """
                                                <template pp-component="input_demo">
                                                  <div pp-component="input_demo">
                                                    <div th:replace="~{components/ui/input :: text(id='name', label='Name', name='name', placeholder='Enter name...', errorExpr='nameError')}"></div>
                                                    <script>
                                                      const [nameError, setNameError] = pp.state('');
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                register("label", "Label", "Form",
                                "Accessible form label with optional required indicator.",
                                List.of("default", "inline"),
                                List.of(
                                                param("forId", "String", null, true,
                                                                "ID of the form element this label is for"),
                                                param("text", "String", null, true, "Label text"),
                                                param("required", "boolean", "false", false,
                                                                "Show required asterisk indicator"),
                                                param("description", "String", "null", false,
                                                                "Helper description text (inline variant)")),
                                "components/ui/label",
                                "<div th:replace=\"~{components/ui/label :: default(forId='email', text='Email Address', required=true)}\"></div>",
                                null,
                                List.of());

                register("checkbox", "Checkbox", "Form",
                                "A checkbox input with label and optional description.",
                                List.of("default", "simple"),
                                List.of(
                                                param("id", "String", null, true, "HTML id attribute"),
                                                param("name", "String", null, true, "Form field name"),
                                                param("label", "String", null, true, "Checkbox label text"),
                                                param("checkedExpr", "String", "null", false,
                                                                "PulsePoint checked state expression"),
                                                param("changeExpr", "String", "null", false,
                                                                "onchange handler expression"),
                                                param("description", "String", "null", false,
                                                                "Helper description text (default variant)")),
                                "components/ui/checkbox",
                                "<div th:replace=\"~{components/ui/checkbox :: default(id='agree', name='agree', label='I agree to terms', checkedExpr='isAgreed', changeExpr='toggleAgreed()')}\"></div>",
                                """
                                                <template pp-component="checkbox_demo">
                                                  <div pp-component="checkbox_demo">
                                                    <div th:replace="~{components/ui/checkbox :: default(id='agree', name='agree', label='Accept terms', description='You must accept to continue', checkedExpr='isAgreed', changeExpr='setIsAgreed(!isAgreed)')}"></div>
                                                    <script>
                                                      const [isAgreed, setIsAgreed] = pp.state(false);
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                register("select", "Select", "Form",
                                "A dropdown select input with options.",
                                List.of("default", "mapped"),
                                List.of(
                                                param("id", "String", null, true, "HTML id attribute"),
                                                param("label", "String", null, false, "Label text above the select"),
                                                param("name", "String", null, true, "Form field name"),
                                                param("options", "List", null, true, "List of option values"),
                                                param("valueExpr", "String", "null", false,
                                                                "PulsePoint value binding expression"),
                                                param("changeExpr", "String", "null", false,
                                                                "onchange handler expression"),
                                                param("placeholder", "String", "null", false,
                                                                "Placeholder option text")),
                                "components/ui/select",
                                "<div th:replace=\"~{components/ui/select :: default(id='status', label='Status', name='status', options=${{'TODO','IN_PROGRESS','DONE'}}, placeholder='Select status...')}\"></div>",
                                null,
                                List.of());

                register("switch", "Switch", "Form",
                                "A toggle switch for boolean on/off states.",
                                List.of("default", "small", "standalone"),
                                List.of(
                                                param("label", "String", null, false, "Switch label text"),
                                                param("checkedExpr", "String", null, true,
                                                                "PulsePoint state expression for checked/unchecked"),
                                                param("toggleExpr", "String", null, true,
                                                                "onclick handler to toggle state"),
                                                param("description", "String", "null", false,
                                                                "Helper description text")),
                                "components/ui/switch",
                                "<div th:replace=\"~{components/ui/switch :: default(label='Notifications', checkedExpr='notifEnabled', toggleExpr='setNotifEnabled(!notifEnabled)')}\"></div>",
                                """
                                                <template pp-component="switch_demo">
                                                  <div pp-component="switch_demo">
                                                    <div th:replace="~{components/ui/switch :: default(label='Dark Mode', checkedExpr='isDark', toggleExpr='setIsDark(!isDark)')}"></div>
                                                    <script>
                                                      const [isDark, setIsDark] = pp.state(true);
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                // ── Data Display Components ──
                register("badge", "Badge", "Data Display",
                                "Displays status and priority indicator badges.",
                                List.of("status-badges", "priority-badges"),
                                List.of(
                                                param("statusField", "String", null, true,
                                                                "PulsePoint expression for status value (TODO/IN_PROGRESS/DONE)"),
                                                param("priorityField", "String", null, true,
                                                                "PulsePoint expression for priority value (HIGH/MEDIUM/LOW)")),
                                "components/ui/badge",
                                "<div th:replace=\"~{components/ui/badge :: status-badges(statusField='task.status')}\"></div>",
                                null,
                                List.of());

                register("avatar", "Avatar", "Data Display",
                                "Displays a user avatar with initial letter and optional role text.",
                                List.of("default", "small", "large", "group"),
                                List.of(
                                                param("name", "String", null, true,
                                                                "User's name (first letter used as initial)"),
                                                param("role", "String", "null", false, "User's role or subtitle text"),
                                                param("bgClass", "String",
                                                                "bg-gradient-to-tr from-indigo-600 to-purple-600",
                                                                false, "Background CSS class for avatar circle")),
                                "components/ui/avatar",
                                "<div th:replace=\"~{components/ui/avatar :: default(name='Admin User', role='Administrator')}\"></div>",
                                null,
                                List.of());

                register("table", "Table", "Data Display",
                                "A styled data table with header, rows, cells, and empty state.",
                                List.of("default", "compact", "row", "cell", "empty"),
                                List.of(
                                                param("columns", "List<String>", null, true,
                                                                "List of column header names"),
                                                param("clickExpr", "String", "null", false,
                                                                "Row click handler (row variant)"),
                                                param("text", "String", null, false,
                                                                "Cell text content (cell variant)"),
                                                param("align", "String", "left", false,
                                                                "Cell alignment: left/center/right"),
                                                param("message", "String", "No data available", false,
                                                                "Empty state message"),
                                                param("colSpan", "int", "4", false, "Column span for empty state row")),
                                "components/ui/table",
                                "<div th:replace=\"~{components/ui/table :: default(columns=${{#lists.toList('Name','Status','Priority','Actions')}})}\">\n  <!-- rows here -->\n</div>",
                                null,
                                List.of());

                register("kbd", "Kbd", "Data Display",
                                "Displays a keyboard shortcut key indicator.",
                                List.of("default", "combo"),
                                List.of(
                                                param("text", "String", null, true, "Key label text (single key)"),
                                                param("keys", "List<String>", null, true,
                                                                "List of key labels (combo variant)")),
                                "components/ui/kbd",
                                "<div th:replace=\"~{components/ui/kbd :: default(text='⌘')}\"></div>",
                                null,
                                List.of());

                // ── Layout Components ──
                register("card", "Card", "Layout",
                                "A container component with optional header, body, and footer sections.",
                                List.of("default", "full", "compact", "interactive"),
                                List.of(
                                                param("title", "String", "null", false, "Card title text"),
                                                param("description", "String", "null", false,
                                                                "Card description/subtitle text"),
                                                param("footerAlign", "String", "start", false,
                                                                "Footer alignment: start/center/right (full variant)"),
                                                param("clickExpr", "String", "null", false,
                                                                "onclick handler (interactive variant)")),
                                "components/ui/card",
                                "<div th:replace=\"~{components/ui/card :: default(title='Card Title', description='Card description text')}\"></div>",
                                null,
                                List.of());

                register("separator", "Separator", "Layout",
                                "A visual divider between content sections.",
                                List.of("horizontal", "vertical", "line"),
                                List.of(
                                                param("label", "String", "null", false,
                                                                "Optional label text in the center (horizontal variant)"),
                                                param("height", "String", "h-6", false,
                                                                "Height class for vertical separator")),
                                "components/ui/separator",
                                "<div th:replace=\"~{components/ui/separator :: horizontal(label='or')}\"></div>",
                                null,
                                List.of());

                register("typography", "Typography", "Layout",
                                "Semantic text elements: headings, paragraphs, code, blockquotes.",
                                List.of("h1", "h2", "h3", "h4", "p", "lead", "muted", "code", "blockquote", "section"),
                                List.of(
                                                param("text", "String", null, true, "Text content")),
                                "components/ui/typography",
                                "<div th:replace=\"~{components/ui/typography :: h2(text='Section Title')}\"></div>",
                                null,
                                List.of());

                register("accordion", "Accordion", "Layout",
                                "Collapsible content sections for showing/hiding content.",
                                List.of("item", "group"),
                                List.of(
                                                param("id", "String", null, true, "Unique accordion item identifier"),
                                                param("title", "String", null, true, "Accordion header title"),
                                                param("openExpr", "String", null, true,
                                                                "PulsePoint state expression for open/closed"),
                                                param("toggleExpr", "String", null, true,
                                                                "onclick handler to toggle open state")),
                                "components/ui/accordion",
                                "<div th:replace=\"~{components/ui/accordion :: item(id='acc-1', title='What is PulsePoint?', openExpr='isOpen', toggleExpr='setIsOpen(!isOpen)')}\">Content here</div>",
                                """
                                                <template pp-component="accordion_demo">
                                                  <div pp-component="accordion_demo">
                                                    <div th:replace="~{components/ui/accordion :: item(id='acc-1', title='FAQ Item', openExpr='isOpen', toggleExpr='setIsOpen(!isOpen)')}">
                                                      Answer content goes here.
                                                    </div>
                                                    <script>
                                                      const [isOpen, setIsOpen] = pp.state(false);
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                register("tabs", "Tabs", "Navigation",
                                "Tab panel navigation for switching between content views.",
                                List.of("default", "pills", "vertical"),
                                List.of(
                                                param("tabList", "List<String>", null, true, "List of tab labels"),
                                                param("activeTabExpr", "String", null, true,
                                                                "PulsePoint state expression for active tab"),
                                                param("changeTabExpr", "String", null, true,
                                                                "Function name to call when tab changes")),
                                "components/ui/tabs",
                                "<div th:replace=\"~{components/ui/tabs :: default(tabList=${{#lists.toList('Overview','Settings','Activity')}}, activeTabExpr='activeTab', changeTabExpr='setActiveTab')}\"></div>",
                                """
                                                <template pp-component="tabs_demo">
                                                  <div pp-component="tabs_demo">
                                                    <div th:replace="~{components/ui/tabs :: default(tabList=${{#lists.toList('Overview','Settings')}}, activeTabExpr='activeTab', changeTabExpr='setActiveTab')}"></div>
                                                    <div hidden="{activeTab !== 'Overview'}">Overview content</div>
                                                    <div hidden="{activeTab !== 'Settings'}">Settings content</div>
                                                    <script>
                                                      const [activeTab, setActiveTab] = pp.state('Overview');
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                // ── Feedback Components ──
                register("alert", "Alert", "Feedback",
                                "Displays success or error alert banners with dismiss button.",
                                List.of("success", "error"),
                                List.of(
                                                param("messageExpr", "String", null, true,
                                                                "PulsePoint state expression for alert message"),
                                                param("clearClickExpr", "String", null, true,
                                                                "onclick handler to clear/dismiss the alert")),
                                "components/ui/alert",
                                "<div th:replace=\"~{components/ui/alert :: success(messageExpr='successMsg', clearClickExpr='setSuccessMsg(\\'\\')')}\"></div>",
                                null,
                                List.of());

                register("progress", "Progress", "Feedback",
                                "Progress indicator with bar, slim, stepped, and circular variants.",
                                List.of("default", "slim", "steps", "circular"),
                                List.of(
                                                param("percentExpr", "String", null, true,
                                                                "PulsePoint state expression for percentage (0–100)"),
                                                param("label", "String", "null", false,
                                                                "Label text above the progress bar"),
                                                param("currentStep", "String", null, false,
                                                                "Current step expression (steps variant)"),
                                                param("totalSteps", "int", "5", false,
                                                                "Total number of steps (steps variant)"),
                                                param("size", "String", "md", false,
                                                                "Size: sm/md/lg (circular variant)")),
                                "components/ui/progress",
                                "<div th:replace=\"~{components/ui/progress :: default(percentExpr='progressVal', label='Upload Progress')}\"></div>",
                                """
                                                <template pp-component="progress_demo">
                                                  <div pp-component="progress_demo">
                                                    <div th:replace="~{components/ui/progress :: default(percentExpr='progress', label='Loading')}"></div>
                                                    <script>
                                                      const [progress, setProgress] = pp.state(45);
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of());

                register("spinner", "Spinner", "Feedback",
                                "Loading spinner indicators with circle, dots, and bar variants.",
                                List.of("default", "dots", "bar"),
                                List.of(
                                                param("size", "String", "md", false, "Size: sm/md/lg"),
                                                param("label", "String", "null", false,
                                                                "Loading text label (default variant)"),
                                                param("hiddenExpr", "String", "null", false,
                                                                "PulsePoint expression to hide spinner when done")),
                                "components/ui/spinner",
                                "<div th:replace=\"~{components/ui/spinner :: default(size='md', label='Loading...')}\"></div>",
                                null,
                                List.of());

                register("skeleton", "Skeleton", "Feedback",
                                "Loading placeholder shapes: line, block, circle, card, table row.",
                                List.of("line", "block", "circle", "card", "tableRow"),
                                List.of(
                                                param("widthClass", "String", "w-full", false, "Width CSS class"),
                                                param("heightClass", "String", "h-24", false,
                                                                "Height CSS class (block variant)"),
                                                param("size", "String", "md", false, "Size: sm/md/lg (circle variant)"),
                                                param("cols", "int", "4", false,
                                                                "Number of columns (tableRow variant)")),
                                "components/ui/skeleton",
                                "<div th:replace=\"~{components/ui/skeleton :: card}\"></div>",
                                null,
                                List.of());

                register("toast", "Toast", "Feedback",
                                "Temporary notification messages with auto-dismiss support.",
                                List.of("success", "error", "info", "warning"),
                                List.of(
                                                param("messageExpr", "String", null, true,
                                                                "PulsePoint state expression for message text"),
                                                param("visibleExpr", "String", null, true,
                                                                "PulsePoint state expression for visibility"),
                                                param("closeExpr", "String", null, true,
                                                                "onclick handler to close the toast")),
                                "components/ui/toast",
                                "<div th:replace=\"~{components/ui/toast :: success(messageExpr='toastMsg', visibleExpr='showToast', closeExpr='setShowToast(false)')}\"></div>",
                                """
                                                <template pp-component="toast_demo">
                                                  <div pp-component="toast_demo">
                                                    <div th:replace="~{components/ui/button :: primary(text='Show Toast', clickExpr='showNotification()')}"></div>
                                                    <div th:replace="~{components/ui/toast :: success(messageExpr='toastMsg', visibleExpr='showToast', closeExpr='setShowToast(false)')}"></div>
                                                    <script>
                                                      const [showToast, setShowToast] = pp.state(false);
                                                      const [toastMsg, setToastMsg] = pp.state('');
                                                      const showNotification = () => {
                                                        setToastMsg('Operation completed successfully!');
                                                        setShowToast(true);
                                                        setTimeout(() => setShowToast(false), 3000);
                                                      };
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of("button"));

                // ── Overlay Components ──
                register("dialog", "Dialog", "Overlay",
                                "A modal dialog that interrupts the user with important content.",
                                List.of("default", "confirm", "form"),
                                List.of(
                                                param("id", "String", null, true, "Unique dialog identifier"),
                                                param("title", "String", null, true, "Dialog title text"),
                                                param("description", "String", "null", false,
                                                                "Dialog description text"),
                                                param("openExpr", "String", null, true,
                                                                "PulsePoint state expression for open/closed"),
                                                param("closeExpr", "String", null, true,
                                                                "onclick handler to close the dialog"),
                                                param("onConfirmExpr", "String", "null", false,
                                                                "Confirm action handler (confirm variant)"),
                                                param("onCancelExpr", "String", "null", false,
                                                                "Cancel action handler (confirm variant)"),
                                                param("message", "String", "null", false,
                                                                "Confirmation message (confirm variant)"),
                                                param("destructive", "boolean", "false", false,
                                                                "Use destructive (red) confirm button style")),
                                "components/ui/dialog",
                                "<div th:replace=\"~{components/ui/dialog :: confirm(id='del', title='Delete Task', message='This action cannot be undone.', openExpr='isDialogOpen', onConfirmExpr='handleDelete()', onCancelExpr='setIsDialogOpen(false)', destructive=true)}\"></div>",
                                """
                                                <template pp-component="dialog_demo">
                                                  <div pp-component="dialog_demo">
                                                    <div th:replace="~{components/ui/button :: primary(text='Open Dialog', clickExpr='setIsOpen(true)')}"></div>
                                                    <div th:replace="~{components/ui/dialog :: confirm(id='demo', title='Confirm Action', message='Are you sure?', openExpr='isOpen', onConfirmExpr='handleConfirm()', onCancelExpr='setIsOpen(false)')}"></div>
                                                    <script>
                                                      const [isOpen, setIsOpen] = pp.state(false);
                                                      const handleConfirm = () => {
                                                        setIsOpen(false);
                                                        // perform action
                                                      };
                                                    </script>
                                                  </div>
                                                </template>""",
                                List.of("button"));

                register("tooltip", "Tooltip", "Overlay",
                                "A popup that displays information on hover.",
                                List.of("default", "bottom", "left", "right"),
                                List.of(
                                                param("text", "String", null, true, "Tooltip text content")),
                                "components/ui/tooltip",
                                "<div th:replace=\"~{components/ui/tooltip :: default(text='This is a tooltip')}\">Hover me</div>",
                                null,
                                List.of());
        }

        private void register(String name, String displayName, String category,
                        String description, List<String> variants,
                        List<Map<String, String>> parameters, String sourceFile,
                        String usageExample, String reactiveExample,
                        List<String> dependencies) {
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("name", name);
                comp.put("displayName", displayName);
                comp.put("category", category);
                comp.put("description", description);
                comp.put("variants", variants);
                comp.put("parameters", parameters);
                comp.put("sourceFile", sourceFile);
                comp.put("usageExample", usageExample);
                comp.put("reactiveExample", reactiveExample);
                comp.put("dependencies", dependencies);
                comp.put("installCommand", "ppui add " + name);
                componentRegistry.put(name, comp);
                componentList.add(comp);
        }

        private Map<String, String> param(String name, String type, String defaultValue,
                        boolean required, String description) {
                Map<String, String> p = new LinkedHashMap<>();
                p.put("name", name);
                p.put("type", type);
                p.put("default", defaultValue != null ? defaultValue : "—");
                p.put("required", String.valueOf(required));
                p.put("description", description);
                return p;
        }
}
