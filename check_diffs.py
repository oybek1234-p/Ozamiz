import subprocess

files = [
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\home\HomeViewModel.kt",
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\home\HomeFragment.kt",
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\log\food\LogFoodFragment.kt",
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\log\food\LogFoodViewModel.kt",
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\menu\detail\MenuDetailFragment.kt",
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\onboarding\screens\TargetWeightFragment.kt",
    r"c:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\feature\profile\CalorieSettingsFragment.kt"
]

for f in files:
    try:
        diff = subprocess.check_output(['git', '--no-pager', 'diff', 'HEAD', '--', f]).decode('utf-8')
        if diff:
            print(f"--- {f.split('\\')[-1]} ---")
            print(diff)
    except Exception as e:
        pass
