// eslint-disable-next-line import/prefer-default-export
export const convertFormMapToMap = (formMap) => {
  const map = {};
  for (const entry of formMap) {
    map[entry.key] = entry.value;
  }
  return map;
};
