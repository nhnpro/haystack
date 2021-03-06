package haystack.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import haystack.core.LanguageResolver;
import haystack.core.models.ClassModel;
import haystack.core.models.FieldModel;
import haystack.core.models.PageModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ModelTableDialog extends JDialog implements ClassesListDelegate.OnClassSelectedListener {

    private static final int ANNOTATIONS_NONE = 0;
    private static final int ANNOTATIONS_GSON = 1;
    private static final int ANNOTATIONS_FAST_JSON = 2;
    private static final int ANNOTATIONS_MOSHI = 3;
    private static final int ANNOTATIONS_JACKSON = 4;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable fieldsTable;
    private JTable table1;
    private JLabel claasesListLabel;
    private JPanel bottom;
    private JPanel field;
    private JPanel classT;

    private FieldsTableDelegate fieldsTableDelegate;
    private ClassesListDelegate classesListDelegate;

    private List<ClassModel> classModelList;
    private PageModel pageModel;

    private ModelTableCallbacks callbacks;

    private int currentSelectedClassIndex = 0;

    private HashMap<String, String> classNames;

    private TextResources textResources;

    public ModelTableDialog(PageModel pageModel, LanguageResolver resolver,
                            TextResources textResources, ModelTableCallbacks callbacks) {
        this.classModelList = pageModel.classModels;
        this.pageModel = pageModel;
        this.callbacks = callbacks;
        this.textResources = textResources;
        init();

        classNames = new HashMap<>();

        for (ClassModel classModel : classModelList) {
            classNames.put(classModel.getName(), classModel.getName());
        }

        classesListDelegate = new ClassesListDelegate(table1, classModelList, classNames, this);
        fieldsTableDelegate = new FieldsTableDelegate(fieldsTable, resolver, textResources);
        fieldsTableDelegate.setClass(classModelList.get(0));
        claasesListLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        setTitle(textResources.getFieldsDialogTitle());
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> dispose());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        contentPane.registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }


    private void onOK() {
        if (callbacks != null) {
            boolean unique = false;
            for (ClassModel classModel : classModelList) {
                String className = classNames.get(classModel.getName());
                if (className != null) {
                    classModel.setName(className);
                }
                Iterator<FieldModel> iterator = classModel.getFields().iterator();
                while (iterator.hasNext()) {
                    FieldModel field = iterator.next();
                    if (!field.isEnabled() && !field.isUnique()) {
                        iterator.remove();
                    } else {
                        if (field.isUnique()) {
                            classModel.setUniqueField(field.getName());
                            classModel.setUniqueFieldType(field.getType());
                            unique = true;
                        }
                        if (!field.getType().equals("int") && !field.getType().equals("double") && !field.getType().equals("String") && !field.getType().equals("bool")) {
                            field.setDefaultValue("null");
                        }
                        String fieldClassName = classNames.get(field.getType());
                        if (fieldClassName != null) {
                            field.setType(fieldClassName);
                        }
                    }
                }
            }
            if (!unique){
                Messages.showMessageDialog(pageModel.modelName+" class must have a unique field！", "Error", Messages.getErrorIcon());
                return;
            }
            pageModel.classModels = classModelList;
            callbacks.onModelsReady(pageModel);
            dispose();
        }
    }

    @Override
    public void onClassSelected(ClassModel classData, int index) {
        classModelList.get(currentSelectedClassIndex).setFields(fieldsTableDelegate.getFieldsData());
        currentSelectedClassIndex = index;
        fieldsTableDelegate.setClass(classData);
    }

    public interface ModelTableCallbacks {
        void onModelsReady(PageModel pageModel);
    }
}
