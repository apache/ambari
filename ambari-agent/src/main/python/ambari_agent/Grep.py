class Grep:

  def __init__(self):
    pass

  def grep(self, string, phrase, before, after):
    """
    Tries to find the last occurence of phrase in a given string. String should consist of lines,
    separated by line separators. String trim is performed before search.
    If occurence is found, grep () takes not more than 'before' lines above and 'after' lines below.
    If phrase is not found, returns None. Search is not case sensitive, regexps are not supported.
    """
    stripped_string = string.strip()
    lines = stripped_string.splitlines(True)
    last_occurence = None
    for index in range(len(lines)):
      line = lines[index]
      if phrase.lower() in line.lower():
        last_occurence = index
    if last_occurence is None:
      return None
    bound_a = before
    if last_occurence < before:
      bound_a = last_occurence
    result = None
    if (len(lines) - last_occurence) < after:
      result = lines[last_occurence - bound_a :]
    else:
      result = lines[last_occurence - bound_a : last_occurence + after + 1]
    return "".join(result).strip()


  def tail(self, string, n):
    """
    Copies last n lines from string to result. Also, string trim is performed.
    """
    stripped_string = string.strip()
    lines = stripped_string.splitlines(True)
    if len(lines) <= n:
      return stripped_string
    else:
      length = len(lines)
      tailed = lines[length - n:]
      return "".join(tailed)
