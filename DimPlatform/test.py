import os
import json
import re
from datetime import datetime


def parse_http_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()

    requests = content.split('###')[1:]
    parsed_requests = []

    for request in requests:
        lines = request.strip().split('\n')
        first_line = lines[0].strip()
        if ' ' in first_line:
            method, url = first_line.split(' ', 1)
        else:
            method = first_line
            url = "未指定URL"

        headers = {}
        body = ''
        response_file = None

        header_end = next((i for i, line in enumerate(lines) if line.strip() == ''), len(lines))
        for line in lines[1:header_end]:
            if ': ' in line:
                key, value = line.split(': ', 1)
                headers[key] = value

        body_start = header_end + 1
        body_end = next((i for i in range(body_start, len(lines)) if lines[i].startswith('<>')), len(lines))

        if body_start < body_end:
            body = '\n'.join(lines[body_start:body_end]).strip()

        response_match = re.search(r'<>\s*(.*\.txt)', request)
        if response_match:
            response_file = response_match.group(1).strip()

        parsed_requests.append((method, url, headers, body, response_file))

    return parsed_requests


def parse_json_body(body):
    print(f"Attempting to parse JSON body: {body}")  # 调试信息
    try:
        json_body = json.loads(body)
        params = extract_params(json_body)
        print(f"Extracted params: {params}")  # 调试信息
        return params
    except json.JSONDecodeError as e:
        print(f"JSON解析错误: {e}")  # 错误信息
        return []


def extract_params(obj, prefix=''):
    params = []
    if isinstance(obj, dict):
        for key, value in obj.items():
            new_prefix = f"{prefix}.{key}" if prefix else key
            if isinstance(value, (dict, list)):
                params.extend(extract_params(value, new_prefix))
            else:
                params.append({
                    'name': new_prefix,
                    'type': type(value).__name__,
                    'description': '',
                    'required': '',
                    'example': str(value)
                })
    elif isinstance(obj, list) and len(obj) > 0:
        params.extend(extract_params(obj[0], f"{prefix}[]"))
    return params


def get_latest_response_file(response_file):
    if not response_file:
        return None

    dir_path = os.path.dirname(os.path.abspath(__file__))

    pattern = re.compile(rf'{re.escape(response_file.split(".")[0])}\..*\.txt')

    try:
        matching_files = [f for f in os.listdir(dir_path) if pattern.match(f)]
    except OSError as e:
        print(f"错误: 无法列出目录 '{dir_path}' 的内容: {e}")
        return None

    if not matching_files:
        print(f"警告: 在 '{dir_path}' 中没有找到匹配的响应文件")
        return None

    latest_file = max(matching_files, key=lambda f: os.path.getmtime(os.path.join(dir_path, f)))
    return os.path.join(dir_path, latest_file)


def generate_md_doc(http_file_path):
    parsed_requests = parse_http_file(http_file_path)

    md = "# API 接口文档\n\n"

    for index, (method, url, headers, body, response_file) in enumerate(parsed_requests, 1):
        md += f"## 接口 {index}\n\n"
        md += f"### 请求URL\n\n`{method} {url}`\n\n"

        if headers:
            md += "### 请求头\n\n"
            for key, value in headers.items():
                md += f"- **{key}**: {value}\n"
            md += "\n"

        if body:
            md += "### 请求体\n\n```json\n"
            md += body
            md += "\n```\n\n"

            md += "#### 参数列表\n\n"
            md += "| 字段名 | 类型 | 描述 | 是否必填 | 示例 |\n"
            md += "| ------ | ---- | ---- | -------- | ---- |\n"

            params = parse_json_body(body)
            if params:
                for param in params:
                    md += f"| {param['name']} | {param['type']} | {param['description']} | {param['required']} | {param['example']} |\n"
            else:
                md += "| 无参数 | - | - | - | - |\n"

            md += "\n"

        if response_file:
            latest_response_file = get_latest_response_file(response_file)
            if latest_response_file and os.path.exists(latest_response_file):
                md += "### 响应示例\n\n```\n"
                with open(latest_response_file, 'r', encoding='utf-8') as file:
                    md += file.read()
                md += "\n```\n\n"
            else:
                md += "### 响应\n\n无法获取响应示例。\n\n"
        else:
            md += "### 响应\n\n响应内容将根据实际情况而定。\n\n"

    return md


# 使用示例
http_file_path = 'C:\\Users\\rog\\AppData\\Roaming\\JetBrains\\IntelliJIdea2024.2\\scratches\\generated-requests.http'
md_doc = generate_md_doc(http_file_path)
print(md_doc)

# 保存Markdown文档
output_file = 'api_doc.md'
with open(output_file, 'w', encoding='utf-8') as file:
    file.write(md_doc)
print(f"Markdown文档已保存至 {output_file}")