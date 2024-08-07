理解了，我们将修改脚本只输出目录，不包括文件。这里是更新后的 Python 脚本：

```python
import os

def generate_directory_structure(root_path, max_depth=4):
    """
    生成目录结构并返回 Markdown 表格格式的字符串
    
    :param root_path: 根目录路径
    :param max_depth: 最大深度 (默认为4)
    :return: Markdown 表格格式的字符串
    """
    table = "| 第一层 | 第二层 | 第三层 | 第四层 |\n|--------|--------|--------|--------|\n"
    
    for root, dirs, _ in os.walk(root_path):
        # 计算当前目录的深度
        depth = root[len(root_path):].count(os.sep)
        
        # 如果深度超过最大深度，跳过
        if depth >= max_depth:
            dirs.clear()  # 防止继续遍历更深层次的目录
            continue
        
        # 获取当前目录名
        dir_name = os.path.basename(root)
        
        # 创建表格行
        row = [""] * max_depth
        row[depth] = dir_name
        
        # 将行添加到表格中
        table += "| " + " | ".join(row) + " |\n"
    
    return table

# 获取当前目录
current_dir = os.getcwd()

# 生成目录结构表格
markdown_table = generate_directory_structure(current_dir)

# 打印 Markdown 表格
print(markdown_table)
```

这个更新后的脚本有以下几点改动：

1. 在 `os.walk()` 中，我们使用了 `_` 来忽略文件列表，因为我们不需要处理文件。

2. 移除了之前用于添加文件到表格的代码。

3. 在达到最大深度时，我们使用 `dirs.clear()` 来防止 `os.walk()` 继续遍历更深层次的目录，这可以提高脚本的效率。

4. 其他部分保持不变，仍然按照 4 层目录结构输出 Markdown 表格。

使用方法与之前相同，将脚本保存为 .py 文件（例如 `directory_structure.py`），然后在命令行中运行：

```
python directory_structure.py
```

这将输出一个 Markdown 格式的表格，仅显示当前目录下 4 层的目录结构，不包括文件。

如果您需要进一步的修改或解释，请随时告诉我。