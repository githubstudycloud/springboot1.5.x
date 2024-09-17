## Prompt:
undefined

## Response:
undefined

## Prompt:
idea2023使用groovy脚本配置方法注释，能自动判断注解存在，添加描述，日期，作者，参数，返回，异常

## Response:
我可以为您提供一个Groovy脚本,用于在IntelliJ IDEA 2023中自动为方法添加注释。这个脚本可以检查现有注解,并根据需要添加描述、日期、作者、参数、返回值和异常信息。以下是一个示例实现:

```groovy
Copyimport com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil
import java.text.SimpleDateFormat

class AddMethodCommentAction extends AnAction {
    @Override
    void actionPerformed(AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE)
        Editor editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile == null || editor == null) return

        PsiElement element = psiFile.findElementAt(editor.caretModel.offset)
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class)
        if (method == null) return

        WriteCommandAction.runWriteCommandAction(psiFile.project) {
            addOrUpdateMethodComment(method)
        }
    }

    private void addOrUpdateMethodComment(PsiMethod method) {
        PsiElementFactory factory = JavaPsiFacade.getInstance(method.project).elementFactory
        PsiDocComment existingComment = method.docComment

        StringBuilder commentText = new StringBuilder()
        commentText.append("/**\n")
        commentText.append(" * TODO: Add method description\n")
        commentText.append(" *\n")
        commentText.append(" * @author ${System.getProperty("user.name")}\n")
        commentText.append(" * @date ${new SimpleDateFormat("yyyy-MM-dd").format(new Date())}\n")

        for (PsiParameter param : method.parameterList.parameters) {
            commentText.append(" * @param ${param.name} TODO: Add parameter description\n")
        }

        if (method.returnType != PsiType.VOID) {
            commentText.append(" * @return TODO: Add return value description\n")
        }

        for (PsiClassType exception : method.throwsList.referencedTypes) {
            commentText.append(" * @throws ${exception.className} TODO: Add exception description\n")
        }

        commentText.append(" */")

        PsiDocComment newComment = factory.createDocCommentFromText(commentText.toString())
        if (existingComment != null) {
            existingComment.replace(newComment)
        } else {
            method.addBefore(newComment, method.firstChild)
        }
    }
}
```

这个脚本创建了一个名为`AddMethodCommentAction`的自定义操作。以下是它的主要功能:

1. 检查光标所在位置是否在一个方法内。
2. 如果存在方法,它会创建或更新该方法的文档注释。
3. 自动添加以下内容:
   * TODO标记的方法描述占位符
   * 当前用户名作为作者
   * 当前日期
   * 方法参数(如果有),带有TODO标记的描述占位符
   * 返回值(如果不是void),带有TODO标记的描述占位符
   * 抛出的异常(如果有),带有TODO标记的描述占位符
4. 如果已存在注释,它会更新现有注释;否则,会在方法开头添加新的注释。

要使用这个脚本,你需要在IDEA中注册它作为一个自定义操作。然后,你可以为这个操作分配一个快捷键,或通过菜单/工具栏来触发它。

您想了解这个脚本的任何特定部分,或者需要关于如何在IDEA中设置和使用它的更多信息吗?

## Prompt:
undefined

## Response:
undefined

